package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.request.SvgFileDealerPermissionRequest;
import com.example.svgmanager.dto.response.*;
import com.example.svgmanager.entity.*;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.mapper.SvgMapper;
import com.example.svgmanager.mapper.VehicleConfigurationMapper;
import com.example.svgmanager.repository.*;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.CategoryService;
import com.example.svgmanager.service.FileStorageService;
import com.example.svgmanager.service.SvgSanitizerService;
import com.example.svgmanager.service.SvgService;
import com.example.svgmanager.util.ChecksumUtils;
import com.example.svgmanager.util.FileUtils;
import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class SvgServiceImpl implements SvgService {

    private static final Logger log = LoggerFactory.getLogger(SvgServiceImpl.class);
    private static final int MAX_BATCH_UPLOAD_SIZE = 10;

    private final SvgFileRepository svgFileRepository;
    private final SvgFileVehicleConfigurationRepository svgVehicleConfigRepository;
    private final SvgFileDealerPermissionRepository svgDealerPermissionRepository;
    private final VehicleConfigurationRepository vehicleConfigurationRepository;
    private final DealerRepository dealerRepository;
    private final FileStorageService fileStorageService;
    private final SvgSanitizerService svgSanitizerService;
    private final SvgMapper svgMapper;
    private final VehicleConfigurationMapper vehicleConfigurationMapper;
    private final CurrentUserService currentUserService;
    private final CategoryService categoryService;
    private final AuditLogService auditLogService;
    private final long maxFileSizeBytes;

    public SvgServiceImpl(
            SvgFileRepository svgFileRepository,
            SvgFileVehicleConfigurationRepository svgVehicleConfigRepository,
            SvgFileDealerPermissionRepository svgDealerPermissionRepository,
            VehicleConfigurationRepository vehicleConfigurationRepository,
            DealerRepository dealerRepository,
            FileStorageService fileStorageService,
            SvgSanitizerService svgSanitizerService,
            SvgMapper svgMapper,
            VehicleConfigurationMapper vehicleConfigurationMapper,
            CurrentUserService currentUserService,
            CategoryService categoryService,
            AuditLogService auditLogService,
            @Value("${app.file.max-file-size-bytes:10485760}") long maxFileSizeBytes
    ) {
        this.svgFileRepository = svgFileRepository;
        this.svgVehicleConfigRepository = svgVehicleConfigRepository;
        this.svgDealerPermissionRepository = svgDealerPermissionRepository;
        this.vehicleConfigurationRepository = vehicleConfigurationRepository;
        this.dealerRepository = dealerRepository;
        this.fileStorageService = fileStorageService;
        this.svgSanitizerService = svgSanitizerService;
        this.svgMapper = svgMapper;
        this.vehicleConfigurationMapper = vehicleConfigurationMapper;
        this.currentUserService = currentUserService;
        this.categoryService = categoryService;
        this.auditLogService = auditLogService;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    @Transactional
    public BatchSvgUploadResponse batchUploadSvg(
            List<MultipartFile> files,
            List<Long> vehicleConfigurationIds,
            List<SvgFileDealerPermissionRequest> dealerPermissions
    ) {
        if (!currentUserService.isAdmin()) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền upload file SVG");
        }

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất 1 file SVG để tải lên");
        }

        if (files.size() > MAX_BATCH_UPLOAD_SIZE) {
            throw new BadRequestException("Chỉ được upload tối đa " + MAX_BATCH_UPLOAD_SIZE + " file SVG trong một lần");
        }

        // Validate vehicle configurations if provided
        List<VehicleConfiguration> configsToAssign = new ArrayList<>();
        if (vehicleConfigurationIds != null && !vehicleConfigurationIds.isEmpty()) {
            for (Long configId : vehicleConfigurationIds) {
                VehicleConfiguration vc = vehicleConfigurationRepository.findByIdAndDeletedFalse(configId)
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy cấu hình xe với ID: " + configId));
                configsToAssign.add(vc);
            }
        }

        // Validate dealers and permissions if provided
        Map<Long, Dealer> dealersMap = new HashMap<>();
        if (dealerPermissions != null && !dealerPermissions.isEmpty()) {
            for (SvgFileDealerPermissionRequest dpReq : dealerPermissions) {
                if (dpReq.canDownload() && !dpReq.canView()) {
                    throw new BadRequestException("Không thể cấp quyền tải xuống khi không có quyền xem cho đại lý ID: " + dpReq.dealerId());
                }
                Dealer dealer = dealerRepository.findByIdAndDeletedFalse(dpReq.dealerId())
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy đại lý với ID: " + dpReq.dealerId()));
                dealersMap.put(dpReq.dealerId(), dealer);
            }
        }

        List<String> storedFilePathsToCleanupOnFailure = new ArrayList<>();
        List<SvgResponse> uploadedResponses = new ArrayList<>();
        User currentUser = currentUserService.getCurrentUser();

        try {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    throw new BadRequestException("Tồn tại file rỗng trong danh sách upload");
                }

                if (file.getSize() > maxFileSizeBytes) {
                    throw new BadRequestException("File '" + file.getOriginalFilename() + "' vượt quá dung lượng tối đa cho phép (" + (maxFileSizeBytes / (1024 * 1024)) + " MB)");
                }

                String safeOriginalFilename = FileUtils.getCleanFilename(file.getOriginalFilename());
                if (!FileUtils.isSvgExtension(safeOriginalFilename)) {
                    throw new BadRequestException("Chỉ chấp nhận file định dạng SVG (.svg): " + safeOriginalFilename);
                }

                String contentType = file.getContentType();
                if (contentType != null && !contentType.isBlank()) {
                    String lowerType = contentType.toLowerCase();
                    if (!lowerType.contains("svg") && !lowerType.contains("xml")) {
                        throw new BadRequestException("Content-Type không hợp lệ cho file SVG: " + contentType);
                    }
                }

                byte[] rawBytes;
                try {
                    rawBytes = file.getBytes();
                } catch (IOException e) {
                    log.error("Failed to read bytes from uploaded file: {}", safeOriginalFilename, e);
                    throw new BadRequestException("Không thể đọc nội dung file: " + safeOriginalFilename);
                }

                byte[] sanitizedBytes = svgSanitizerService.sanitizeAndValidateSvg(rawBytes);
                String storedFilename = UUID.randomUUID().toString() + ".svg";
                String filePath = fileStorageService.storeFile(sanitizedBytes, storedFilename);
                storedFilePathsToCleanupOnFailure.add(filePath);

                String checksum = ChecksumUtils.calculateSha256(sanitizedBytes);

                Category defaultCategory = configsToAssign.isEmpty() ? null : configsToAssign.get(0).getCategory();

                SvgFile svgFile = SvgFile.builder()
                        .originalFilename(safeOriginalFilename)
                        .storedFilename(storedFilename)
                        .filePath(filePath)
                        .fileSize((long) sanitizedBytes.length)
                        .contentType("image/svg+xml")
                        .checksum(checksum)
                        .status("ACTIVE")
                        .category(defaultCategory)
                        .uploadedBy(currentUser)
                        .build();

                SvgFile savedSvg = svgFileRepository.save(svgFile);

                // Assign vehicle configurations
                for (VehicleConfiguration vc : configsToAssign) {
                    SvgFileVehicleConfiguration svc = new SvgFileVehicleConfiguration(savedSvg, vc);
                    savedSvg.getVehicleConfigurations().add(svc);
                    svgVehicleConfigRepository.save(svc);
                }

                // Assign dealer permissions
                if (dealerPermissions != null) {
                    for (SvgFileDealerPermissionRequest dpReq : dealerPermissions) {
                        Dealer dealer = dealersMap.get(dpReq.dealerId());
                        if (dealer != null) {
                            SvgFileDealerPermission dp = new SvgFileDealerPermission(savedSvg, dealer, dpReq.canView(), dpReq.canDownload());
                            savedSvg.getDealerPermissions().add(dp);
                            svgDealerPermissionRepository.save(dp);
                        }
                    }
                }

                log.info("[SVG_UPLOADED] Batch uploaded SVG id={}, name='{}', assignedConfigs={}, assignedDealers={}",
                        savedSvg.getId(), savedSvg.getOriginalFilename(), configsToAssign.size(), dealersMap.size());

                auditLogService.log(
                        currentUser.getUsername(),
                        currentUser.getRole().name(),
                        "UPLOAD_SVG",
                        "SvgFile",
                        savedSvg.getId(),
                        "Upload SVG: " + savedSvg.getOriginalFilename() + " (size: " + savedSvg.getFileSize() + " bytes)"
                );

                uploadedResponses.add(svgMapper.toSvgResponse(savedSvg, currentUser, true));
            }
        } catch (Exception e) {
            // Clean up stored files on failure
            for (String path : storedFilePathsToCleanupOnFailure) {
                try {
                    fileStorageService.deleteFile(path);
                } catch (Exception ex) {
                    log.error("Failed to cleanup file during upload rollback: {}", path, ex);
                }
            }
            throw e;
        }

        return new BatchSvgUploadResponse(uploadedResponses, uploadedResponses.size());
    }

    @Override
    @Transactional
    public SvgResponse uploadSvg(MultipartFile file, Long categoryId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("File size exceeds the allowed limit of " + (maxFileSizeBytes / (1024 * 1024)) + "MB");
        }

        if (categoryId == null) {
            throw new BadRequestException("categoryId is required");
        }

        String safeOriginalFilename = FileUtils.getCleanFilename(file.getOriginalFilename());
        if (!FileUtils.isSvgExtension(safeOriginalFilename)) {
            throw new BadRequestException("Chỉ chấp nhận file định dạng SVG (.svg): " + safeOriginalFilename);
        }

        byte[] rawBytes;
        try {
            rawBytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded file content");
        }

        byte[] sanitizedBytes = svgSanitizerService.sanitizeAndValidateSvg(rawBytes);
        String storedFilename = UUID.randomUUID().toString() + ".svg";
        String filePath = fileStorageService.storeFile(sanitizedBytes, storedFilename);
        String checksum = ChecksumUtils.calculateSha256(sanitizedBytes);

        User currentUser = currentUserService.getCurrentUser();
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgent = currentUserService.isAgent();

        User assignedAgent = null;
        if (isAgent && !isAdmin) {
            assignedAgent = currentUser;
        } else if (currentUser.getRole() == Role.USER && currentUser.getAgent() != null) {
            assignedAgent = currentUser.getAgent();
        }

        Category category = null;
        if (categoryId != null) {
            category = categoryService.getCategoryById(categoryId);
        }

        SvgFile svgFile = SvgFile.builder()
                .originalFilename(safeOriginalFilename)
                .storedFilename(storedFilename)
                .filePath(filePath)
                .fileSize((long) sanitizedBytes.length)
                .contentType("image/svg+xml")
                .checksum(checksum)
                .status("ACTIVE")
                .category(category)
                .uploadedBy(currentUser)
                .agent(assignedAgent)
                .build();

        SvgFile saved = svgFileRepository.save(svgFile);

        log.info("[SVG_UPLOADED] SVG uploaded: id={}, originalName='{}', uploadedBy='{}', agentId={}",
                saved.getId(), saved.getOriginalFilename(), currentUser.getUsername(),
                assignedAgent != null ? assignedAgent.getId() : null);

        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole().name(),
                "UPLOAD_SVG",
                "SvgFile",
                saved.getId(),
                "Upload SVG: " + saved.getOriginalFilename()
        );

        return svgMapper.toSvgResponse(saved, currentUser, isAdmin);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SvgResponse> getSvgFiles(
            String keyword,
            String productGroup,
            Long brandId,
            Long modelId,
            Integer year,
            String generationCode,
            Long dealerId,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        User currentUser = currentUserService.getCurrentUser();
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgent = currentUserService.isAgent();

        // Enforce USER / AGENT security scope
        if (!isAdmin) {
            if (isAgent) {
                // Agent must belong to a dealer or have uploaded SVGs
                if (currentUser.getDealer() == null && !svgFileRepository.existsByAgent(currentUser)) {
                    return PageResponse.of(Page.empty());
                }
            } else {
                // Role USER
                if (currentUser.getDealer() == null) {
                    // User has no dealer -> cannot view any files
                    return PageResponse.of(Page.empty());
                }
            }
        }

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String validSortBy = StringUtils.hasText(sortBy) ? sortBy : "createdAt";
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, validSortBy));

        Specification<SvgFile> spec = (root, query, cb) -> {
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            // 1. Data scope isolation
            if (isAdmin) {
                if (dealerId != null) {
                    Join<SvgFile, SvgFileDealerPermission> dpJoin = root.join("dealerPermissions", JoinType.INNER);
                    predicates.add(cb.equal(dpJoin.get("dealer").get("id"), dealerId));
                    predicates.add(cb.isTrue(dpJoin.get("canView")));
                }
            } else if (isAgent) {
                // Agent scope: sees SVGs assigned to their dealer (canView = true) OR uploaded by the agent
                Long agentDealerId = currentUser.getDealer() != null ? currentUser.getDealer().getId() : null;
                if (agentDealerId != null) {
                    Join<SvgFile, SvgFileDealerPermission> dpJoin = root.join("dealerPermissions", JoinType.LEFT);
                    Predicate hasDealerView = cb.and(
                            cb.equal(dpJoin.get("dealer").get("id"), agentDealerId),
                            cb.isTrue(dpJoin.get("canView"))
                    );
                    Predicate isOwnUploaded = cb.equal(root.get("agent").get("id"), currentUser.getId());
                    predicates.add(cb.or(hasDealerView, isOwnUploaded));
                } else {
                    predicates.add(cb.equal(root.get("agent").get("id"), currentUser.getId()));
                }
            } else {
                // USER scope: based on dealer permissions
                Long userDealerId = currentUser.getDealer().getId();
                Join<SvgFile, SvgFileDealerPermission> dpJoin = root.join("dealerPermissions", JoinType.INNER);
                predicates.add(cb.equal(dpJoin.get("dealer").get("id"), userDealerId));
                predicates.add(cb.isTrue(dpJoin.get("canView")));
            }

            // 2. Keyword filter
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(cb.lower(root.get("originalFilename")), "%" + keyword.toLowerCase() + "%"));
            }

            // 3. Status filter
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 4. Vehicle Configuration Filters
            boolean hasVehicleFilter = StringUtils.hasText(productGroup)
                    || brandId != null
                    || modelId != null
                    || year != null
                    || StringUtils.hasText(generationCode);

            if (hasVehicleFilter) {
                Join<SvgFile, SvgFileVehicleConfiguration> svcJoin = root.join("vehicleConfigurations", JoinType.INNER);
                Join<SvgFileVehicleConfiguration, VehicleConfiguration> vcJoin = svcJoin.join("vehicleConfiguration", JoinType.INNER);

                predicates.add(cb.isFalse(vcJoin.get("deleted")));

                if (StringUtils.hasText(productGroup)) {
                    predicates.add(cb.equal(vcJoin.get("productGroup"), ProductGroup.valueOf(productGroup)));
                }
                if (brandId != null) {
                    predicates.add(cb.equal(vcJoin.get("brand").get("id"), brandId));
                }
                if (modelId != null) {
                    predicates.add(cb.equal(vcJoin.get("model").get("id"), modelId));
                }
                if (year != null) {
                    predicates.add(cb.lessThanOrEqualTo(vcJoin.get("yearFrom"), year));
                    predicates.add(cb.greaterThanOrEqualTo(vcJoin.get("yearTo"), year));
                }
                if (StringUtils.hasText(generationCode)) {
                    predicates.add(cb.like(cb.lower(vcJoin.get("generationCode")), "%" + generationCode.toLowerCase() + "%"));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SvgFile> pageResult = svgFileRepository.findAll(spec, pageable);
        return PageResponse.of(pageResult.map(s -> svgMapper.toSvgResponse(s, currentUser, isAdmin)));
    }

    @Override
    @Transactional(readOnly = true)
    public SvgResponse getSvgFileById(Long id) {
        User currentUser = currentUserService.getCurrentUser();
        boolean isAdmin = currentUserService.isAdmin();

        SvgFile svgFile = findAuthorizedSvg(id, false);
        return svgMapper.toSvgResponse(svgFile, currentUser, isAdmin);
    }

    @Override
    @Transactional
    public SvgResponse updateSvg(Long id, String status) {
        if (!currentUserService.isAdmin()) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền chỉnh sửa thông tin SVG");
        }

        SvgFile svgFile = svgFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file SVG với ID: " + id));

        if (StringUtils.hasText(status)) {
            svgFile.setStatus(status);
        }

        SvgFile saved = svgFileRepository.save(svgFile);

        User currentUser = currentUserService.getCurrentUser();
        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole().name(),
                "UPDATE_SVG",
                "SvgFile",
                id,
                "Cập nhật trạng thái file SVG ID: " + id + " -> " + status
        );

        return svgMapper.toSvgResponse(saved, currentUser, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SvgFileDealerPermissionResponse> getDealerPermissions(Long svgFileId) {
        if (!currentUserService.isAdmin()) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền xem danh sách phân quyền đại lý của file SVG");
        }

        SvgFile svgFile = svgFileRepository.findById(svgFileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file SVG với ID: " + svgFileId));

        List<SvgFileDealerPermission> permissions = svgDealerPermissionRepository.findBySvgFileId(svgFileId);
        List<SvgFileDealerPermissionResponse> responses = new ArrayList<>();
        for (SvgFileDealerPermission dp : permissions) {
            if (dp.getDealer() != null && !dp.getDealer().isDeleted()) {
                responses.add(new SvgFileDealerPermissionResponse(
                        dp.getId(),
                        dp.getDealer().getId(),
                        dp.getDealer().getCode(),
                        dp.getDealer().getName(),
                        dp.isCanView(),
                        dp.isCanDownload(),
                        dp.getCreatedAt(),
                        dp.getUpdatedAt()
                ));
            }
        }
        return responses;
    }

    @Override
    @Transactional
    public List<SvgFileDealerPermissionResponse> updateDealerPermissions(Long svgFileId, List<SvgFileDealerPermissionRequest> requests) {
        if (!currentUserService.isAdmin()) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền phân quyền đại lý cho file SVG");
        }

        SvgFile svgFile = svgFileRepository.findById(svgFileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file SVG với ID: " + svgFileId));

        // Delete existing dealer permissions for this SVG
        svgDealerPermissionRepository.deleteBySvgFileId(svgFileId);

        List<SvgFileDealerPermissionResponse> savedResponses = new ArrayList<>();
        if (requests != null) {
            for (SvgFileDealerPermissionRequest req : requests) {
                if (req.canDownload() && !req.canView()) {
                    throw new BadRequestException("Không thể cấp quyền tải xuống khi không có quyền xem cho đại lý ID: " + req.dealerId());
                }

                Dealer dealer = dealerRepository.findByIdAndDeletedFalse(req.dealerId())
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy đại lý với ID: " + req.dealerId()));

                SvgFileDealerPermission dp = new SvgFileDealerPermission(svgFile, dealer, req.canView(), req.canDownload());
                SvgFileDealerPermission saved = svgDealerPermissionRepository.save(dp);

                savedResponses.add(new SvgFileDealerPermissionResponse(
                        saved.getId(),
                        dealer.getId(),
                        dealer.getCode(),
                        dealer.getName(),
                        saved.isCanView(),
                        saved.isCanDownload(),
                        saved.getCreatedAt(),
                        saved.getUpdatedAt()
                ));
            }
        }

        User currentUser = currentUserService.getCurrentUser();
        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole().name(),
                "CHANGE_DEALER_PERMISSION",
                "SvgFile",
                svgFileId,
                "Cập nhật phân quyền đại lý cho file SVG ID: " + svgFileId + " (Số đại lý: " + savedResponses.size() + ")"
        );

        return savedResponses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleConfigurationResponse> getVehicleConfigurations(Long svgFileId) {
        findAuthorizedSvg(svgFileId, false);

        List<SvgFileVehicleConfiguration> assignments = svgVehicleConfigRepository.findBySvgFileId(svgFileId);
        List<VehicleConfigurationResponse> responses = new ArrayList<>();
        for (SvgFileVehicleConfiguration svc : assignments) {
            if (svc.getVehicleConfiguration() != null && !svc.getVehicleConfiguration().isDeleted()) {
                responses.add(vehicleConfigurationMapper.toResponse(svc.getVehicleConfiguration()));
            }
        }
        return responses;
    }

    @Override
    @Transactional
    public List<VehicleConfigurationResponse> updateVehicleConfigurations(Long svgFileId, List<Long> vehicleConfigurationIds) {
        if (!currentUserService.isAdmin()) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền gán cấu hình xe cho file SVG");
        }

        SvgFile svgFile = svgFileRepository.findById(svgFileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file SVG với ID: " + svgFileId));

        svgVehicleConfigRepository.deleteBySvgFileId(svgFileId);

        List<VehicleConfigurationResponse> responses = new ArrayList<>();
        if (vehicleConfigurationIds != null) {
            for (Long configId : vehicleConfigurationIds) {
                VehicleConfiguration vc = vehicleConfigurationRepository.findByIdAndDeletedFalse(configId)
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy cấu hình xe với ID: " + configId));

                SvgFileVehicleConfiguration svc = new SvgFileVehicleConfiguration(svgFile, vc);
                svgVehicleConfigRepository.save(svc);
                responses.add(vehicleConfigurationMapper.toResponse(vc));
            }
        }

        User currentUser = currentUserService.getCurrentUser();
        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole().name(),
                "ASSIGN_VEHICLE_CONFIGURATION",
                "SvgFile",
                svgFileId,
                "Cập nhật gán cấu hình xe cho file SVG ID: " + svgFileId + " (Số cấu hình: " + responses.size() + ")"
        );

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource previewSvg(Long id) {
        SvgFile svgFile = findAuthorizedSvg(id, false);
        Resource resource = fileStorageService.loadFileAsResource(svgFile.getFilePath());
        log.info("[SVG_PREVIEWED] SVG file previewed: id={}, originalName='{}'", id, svgFile.getOriginalFilename());
        return resource;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadSvg(Long id) {
        SvgFile svgFile = findAuthorizedSvg(id, true);
        Resource resource = fileStorageService.loadFileAsResource(svgFile.getFilePath());
        log.info("[SVG_DOWNLOADED] SVG file downloaded: id={}, originalName='{}'", id, svgFile.getOriginalFilename());
        return resource;
    }

    @Override
    @Transactional(readOnly = true)
    public String getOriginalFilename(Long id) {
        return findAuthorizedSvg(id, false).getOriginalFilename();
    }

    @Override
    @Transactional
    public void deleteSvg(Long id) {
        SvgFile svgFile = findAuthorizedSvg(id, false);

        if (!currentUserService.isAdmin()) {
            User currentUser = currentUserService.getCurrentUser();
            if (!currentUserService.isAgent() || svgFile.getAgent() == null || !svgFile.getAgent().getId().equals(currentUser.getId())) {
                throw new ResourceNotFoundException("Không tìm thấy file SVG với ID: " + id);
            }
        }

        String originalFilename = svgFile.getOriginalFilename();

        // 1. Delete associations
        svgVehicleConfigRepository.deleteBySvgFileId(id);
        svgDealerPermissionRepository.deleteBySvgFileId(id);

        // 2. Delete file on storage
        try {
            fileStorageService.deleteFile(svgFile.getFilePath());
        } catch (Exception e) {
            log.warn("Could not delete physical file for SVG ID {}: {}", id, svgFile.getFilePath(), e);
        }

        // 3. Delete database record
        svgFileRepository.delete(svgFile);

        User currentUser = currentUserService.getCurrentUser();
        auditLogService.log(
                currentUser.getUsername(),
                currentUser.getRole().name(),
                "DELETE_SVG",
                "SvgFile",
                id,
                "Xóa file SVG ID: " + id + " ('" + originalFilename + "')"
        );

        log.info("[SVG_DELETED] SVG file deleted: id={}, originalName='{}'", id, originalFilename);
    }

    /**
     * Authorizes and retrieves the SVG file:
     * - ADMIN: full access.
     * - AGENT: can access files belonging to own agent upload OR files granted to the agent's dealer.
     * - USER:
     *     - Must have a dealer.
     *     - Must have canView = true in svg_file_dealer_permissions.
     *     - If requireDownload = true, must also have canDownload = true.
     */
    private SvgFile findAuthorizedSvg(Long id, boolean requireDownload) {
        if (currentUserService.isAdmin()) {
            return svgFileRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file SVG với ID: " + id));
        }

        User currentUser = currentUserService.getCurrentUser();

        // 1. If agent uploaded this SVG themselves, they have full view & download access
        if (currentUserService.isAgent()) {
            Optional<SvgFile> ownSvg = svgFileRepository.findByIdAndAgentId(id, currentUser.getId());
            if (ownSvg.isPresent()) {
                return ownSvg.get();
            }
        }

        // 2. Check dealer permission (applies to both AGENT and USER belonging to a dealer)
        if (currentUser.getDealer() == null) {
            throw new ResourceNotFoundException("Không tìm thấy file SVG với ID: " + id);
        }

        Long userDealerId = currentUser.getDealer().getId();
        SvgFileDealerPermission permission = svgDealerPermissionRepository.findBySvgFileIdAndDealerId(id, userDealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file SVG với ID: " + id));

        if (!permission.isCanView()) {
            throw new ResourceNotFoundException("Không tìm thấy file SVG với ID: " + id);
        }

        if (requireDownload && !permission.isCanDownload()) {
            throw new ForbiddenException("DOWNLOAD_PERMISSION_DENIED: Đại lý của bạn không có quyền tải xuống file này");
        }

        return svgFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file SVG với ID: " + id));
    }
}

package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.SvgResponse;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.mapper.SvgMapper;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.FileStorageService;
import com.example.svgmanager.service.SvgSanitizerService;
import com.example.svgmanager.service.SvgService;
import com.example.svgmanager.util.ChecksumUtils;
import com.example.svgmanager.util.FileUtils;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Kho file SVG — Data Center v2: bỏ phân quyền đại lý theo file và cấu hình xe cũ
 * (board chốt 29/09, Q6). Thợ đăng nhập hợp lệ thấy mọi file; AGENT web vẫn chỉ
 * thấy file mình nạp.
 */
@Service
public class SvgServiceImpl implements SvgService {

    private static final Logger log = LoggerFactory.getLogger(SvgServiceImpl.class);

    private final SvgFileRepository svgFileRepository;
    private final FileStorageService fileStorageService;
    private final SvgSanitizerService svgSanitizerService;
    private final SvgMapper svgMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final long maxFileSizeBytes;

    public SvgServiceImpl(
            SvgFileRepository svgFileRepository,
            FileStorageService fileStorageService,
            SvgSanitizerService svgSanitizerService,
            SvgMapper svgMapper,
            CurrentUserService currentUserService,
            AuditLogService auditLogService,
            @Value("${app.file.max-file-size-bytes:10485760}") long maxFileSizeBytes
    ) {
        this.svgFileRepository = svgFileRepository;
        this.fileStorageService = fileStorageService;
        this.svgSanitizerService = svgSanitizerService;
        this.svgMapper = svgMapper;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    @Transactional
    public SvgResponse uploadSvg(MultipartFile file) {
        User currentUser = currentUserService.getCurrentUser();
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgent = currentUserService.isAgent();

        User assignedAgent = null;
        if (isAgent && !isAdmin) {
            assignedAgent = currentUser;
        } else if (currentUser.getRole() == Role.USER && currentUser.getAgent() != null) {
            assignedAgent = currentUser.getAgent();
        }

        SvgFile saved = storeSvgFile(file, currentUser, assignedAgent, new ArrayList<>());

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

    /** Kiểm + khử độc + lưu một file SVG; ghi nhận đường dẫn đã lưu để rollback. */
    private SvgFile storeSvgFile(MultipartFile file, User currentUser, User assignedAgent,
                                 List<String> storedPaths) {
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
        storedPaths.add(filePath);

        String checksum = ChecksumUtils.calculateSha256(sanitizedBytes);

        SvgFile svgFile = SvgFile.builder()
                .originalFilename(safeOriginalFilename)
                .storedFilename(storedFilename)
                .filePath(filePath)
                .fileSize((long) sanitizedBytes.length)
                .contentType("image/svg+xml")
                .checksum(checksum)
                .status("ACTIVE")
                .uploadedBy(currentUser)
                .agent(assignedAgent)
                .build();

        return svgFileRepository.save(svgFile);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SvgResponse> getSvgFiles(
            String keyword,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        User currentUser = currentUserService.getCurrentUser();
        boolean isAdmin = currentUserService.isAdmin();
        boolean isAgent = currentUserService.isAgent();

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String validSortBy = StringUtils.hasText(sortBy) ? sortBy : "createdAt";
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, validSortBy));

        Specification<SvgFile> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // AGENT chỉ thấy file mình nạp; USER và ADMIN thấy hết (Q6 — không còn quyền đại lý)
            if (!isAdmin && isAgent) {
                predicates.add(cb.equal(root.get("agent").get("id"), currentUser.getId()));
            }

            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(cb.lower(root.get("originalFilename")), "%" + keyword.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
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

        SvgFile svgFile = findAuthorizedSvg(id);
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
    public Resource previewSvg(Long id) {
        SvgFile svgFile = findAuthorizedSvg(id);
        Resource resource = fileStorageService.loadFileAsResource(svgFile.getFilePath());
        log.info("[SVG_PREVIEWED] SVG file previewed: id={}, originalName='{}'", id, svgFile.getOriginalFilename());
        return resource;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadSvg(Long id) {
        SvgFile svgFile = findAuthorizedSvg(id);
        Resource resource = fileStorageService.loadFileAsResource(svgFile.getFilePath());
        log.info("[SVG_DOWNLOADED] SVG file downloaded: id={}, originalName='{}'", id, svgFile.getOriginalFilename());
        return resource;
    }

    @Override
    @Transactional(readOnly = true)
    public String getOriginalFilename(Long id) {
        return findAuthorizedSvg(id).getOriginalFilename();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource thumbnailSvg(Long id) {
        SvgFile svgFile = findAuthorizedSvg(id);
        if (svgFile.getThumbnailPath() == null) {
            throw new ResourceNotFoundException("File không có ảnh xem trước: " + id);
        }
        return fileStorageService.loadFileAsResource(svgFile.getThumbnailPath());
    }

    @Override
    @Transactional(readOnly = true)
    public MediaType thumbnailContentType(Long id) {
        SvgFile svgFile = findAuthorizedSvg(id);
        if (svgFile.getThumbnailPath() == null) {
            throw new ResourceNotFoundException("File không có ảnh xem trước: " + id);
        }
        String ext = FileUtils.getFileExtension(svgFile.getThumbnailPath());
        return switch (ext) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.IMAGE_PNG;
        };
    }

    @Override
    @Transactional
    public void deleteSvg(Long id) {
        SvgFile svgFile = findAuthorizedSvg(id);

        if (!currentUserService.isAdmin()) {
            User currentUser = currentUserService.getCurrentUser();
            if (!currentUserService.isAgent() || svgFile.getAgent() == null || !svgFile.getAgent().getId().equals(currentUser.getId())) {
                throw new ResourceNotFoundException("Không tìm thấy file SVG với ID: " + id);
            }
        }

        String originalFilename = svgFile.getOriginalFilename();

        try {
            fileStorageService.deleteFile(svgFile.getFilePath());
        } catch (Exception e) {
            log.warn("Could not delete physical file for SVG ID {}: {}", id, svgFile.getFilePath(), e);
        }

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
     * Quyền truy cập file (Data Center v2 — Q6: không còn phân quyền đại lý):
     * - ADMIN: mọi file.
     * - AGENT: file mình nạp.
     * - USER: mọi file còn hiệu lực — chỉ còn kiểm phiên.
     */
    private SvgFile findAuthorizedSvg(Long id) {
        if (currentUserService.isAdmin()) {
            return svgFileRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file SVG với ID: " + id));
        }

        User currentUser = currentUserService.getCurrentUser();

        if (currentUserService.isAgent()) {
            Optional<SvgFile> ownSvg = svgFileRepository.findByIdAndAgentId(id, currentUser.getId());
            if (ownSvg.isPresent()) {
                return ownSvg.get();
            }
            throw new ResourceNotFoundException("Không tìm thấy file SVG với ID: " + id);
        }

        return svgFileRepository.findById(id)
                .filter(f -> !"DELETED".equalsIgnoreCase(f.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file SVG với ID: " + id));
    }
}

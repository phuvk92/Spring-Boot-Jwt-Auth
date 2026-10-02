package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.AdminFileResponse;
import com.example.svgmanager.dto.response.AdminFileStatsResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.FileCategory;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.SvgFilePart;
import com.example.svgmanager.entity.SvgFileVehicleNode;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.VehicleNode;
import com.example.svgmanager.entity.VehicleNodeLevel;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ErrorCodes;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.repository.FileCategoryRepository;
import com.example.svgmanager.repository.SvgFilePartRepository;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.SvgFileVehicleNodeRepository;
import com.example.svgmanager.repository.VehicleNodeRepository;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.AdminFileService;
import com.example.svgmanager.service.AuditLogService;
import com.example.svgmanager.service.FileStorageService;
import com.example.svgmanager.service.SvgSanitizerService;
import com.example.svgmanager.svg.SvgImport;
import com.example.svgmanager.svg.SvgImport.ImportedPart;
import com.example.svgmanager.util.ChecksumUtils;
import com.example.svgmanager.util.FileUtils;
import com.example.svgmanager.util.SlugUtils;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Kho part file — SA-DanhMucXe-v2 §3.2/§4.
 *
 * Tách part bằng {@link SvgImport} — port 1:1 của SvgImport.cs bên client nên kết quả
 * khớp đường "thợ tự nạp file" (ca kiểm chốt của issue).
 */
@Service
public class AdminFileServiceImpl implements AdminFileService {

    private static final Logger log = LoggerFactory.getLogger(AdminFileServiceImpl.class);

    private final SvgFileRepository svgFileRepository;
    private final SvgFilePartRepository partRepository;
    private final SvgFileVehicleNodeRepository linkRepository;
    private final VehicleNodeRepository vehicleNodeRepository;
    private final FileCategoryRepository fileCategoryRepository;
    private final FileStorageService fileStorageService;
    private final SvgSanitizerService svgSanitizerService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final long maxFileSizeBytes;

    public AdminFileServiceImpl(
            SvgFileRepository svgFileRepository,
            SvgFilePartRepository partRepository,
            SvgFileVehicleNodeRepository linkRepository,
            VehicleNodeRepository vehicleNodeRepository,
            FileCategoryRepository fileCategoryRepository,
            FileStorageService fileStorageService,
            SvgSanitizerService svgSanitizerService,
            CurrentUserService currentUserService,
            AuditLogService auditLogService,
            @Value("${app.file.max-file-size-bytes:10485760}") long maxFileSizeBytes
    ) {
        this.svgFileRepository = svgFileRepository;
        this.partRepository = partRepository;
        this.linkRepository = linkRepository;
        this.vehicleNodeRepository = vehicleNodeRepository;
        this.fileCategoryRepository = fileCategoryRepository;
        this.fileStorageService = fileStorageService;
        this.svgSanitizerService = svgSanitizerService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    // ── Đọc ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminFileResponse> getFiles(String q, Long categoryId, Integer year,
                                                    Long brandId, Long seriesId, Long modelId,
                                                    int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size),
                Sort.by("updatedAt").descending().and(Sort.by("id").descending()));

        Specification<SvgFile> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), "DELETED"));

            if (StringUtils.hasText(q)) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("originalFilename")), like),
                        cb.like(cb.lower(root.get("rawOriginalFilename")), like),
                        cb.like(cb.lower(root.get("displayName")), like)));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("fileCategory").get("id"), categoryId));
            }
            if (year != null) {
                // Q3 — file không ghi năm hiện với mọi năm khi lọc.
                predicates.add(cb.or(cb.equal(root.get("modelYear"), year),
                        cb.isNull(root.get("modelYear"))));
            }

            // Lọc theo cây: file gắn vào bất kỳ node nào trong nhánh (hãng/dòng/model).
            Long nodeFilter = modelId != null ? modelId
                    : seriesId != null ? seriesId
                    : brandId;
            if (nodeFilter != null) {
                if (!vehicleNodeRepository.existsById(nodeFilter)) {
                    // Id lọc không tồn tại → trang rỗng, không phải lỗi.
                    predicates.add(cb.disjunction());
                } else {
                    List<Long> subtree = vehicleNodeRepository.findSubtreeIds(nodeFilter);
                    Subquery<Long> sq = query.subquery(Long.class);
                    Root<SvgFileVehicleNode> link = sq.from(SvgFileVehicleNode.class);
                    sq.select(link.get("svgFile").get("id"))
                            .where(link.get("vehicleNode").get("id").in(subtree));
                    predicates.add(root.get("id").in(sq));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SvgFile> pageResult = svgFileRepository.findAll(spec, pageable);
        return PageResponse.of(pageResult.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminFileStatsResponse getStats() {
        return new AdminFileStatsResponse(
                svgFileRepository.countByStatus("ACTIVE"),
                linkRepository.countDistinctNodesByFileStatus("ACTIVE"),
                svgFileRepository.countByStatusAndSource("ACTIVE", "DEALER"),
                svgFileRepository.countUnlinkedByStatus("ACTIVE"));
    }

    // ── Ghi ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AdminFileResponse createFile(MultipartFile nestedFile, MultipartFile rawFile,
                                        String name, Long categoryId, Integer year,
                                        List<Long> vehicleNodeIds, MultipartFile thumbnail,
                                        Integer cutAreaLengthMm, Integer cutAreaWidthMm) {
        boolean hasNested = nestedFile != null && !nestedFile.isEmpty();
        boolean hasRaw = rawFile != null && !rawFile.isEmpty();
        if (!hasNested && !hasRaw) {
            throw new BadRequestException("Cần tải lên ít nhất một file (nestedFile hoặc rawFile)",
                    ErrorCodes.FILE_REQUIRED);
        }

        requireName(name);
        FileCategory category = requireCategory(categoryId);
        validateYear(year);
        List<VehicleNode> nodes = resolveVehicleNodes(vehicleNodeIds);

        byte[] nestedBytes = null;
        List<ImportedPart> nestedParts = null;
        if (hasNested) {
            nestedBytes = readAndValidateSvg(nestedFile);
            nestedParts = SvgImport.parseParts(new String(nestedBytes, java.nio.charset.StandardCharsets.UTF_8));
            if (nestedParts.isEmpty()) {
                throw new BadRequestException("File SVG đã xếp không chứa hình kín nào để tách part",
                        ErrorCodes.UNSUPPORTED_FORMAT);
            }
        }

        byte[] rawBytes = null;
        List<ImportedPart> rawParts = null;
        if (hasRaw) {
            rawBytes = readAndValidateSvg(rawFile);
            rawParts = SvgImport.parseParts(new String(rawBytes, java.nio.charset.StandardCharsets.UTF_8));
            if (rawParts.isEmpty()) {
                throw new BadRequestException("File SVG chưa xếp không chứa hình kín nào để tách part",
                        ErrorCodes.UNSUPPORTED_FORMAT);
            }
        }

        if (hasNested && hasRaw && nestedParts.size() != rawParts.size()) {
            throw new BadRequestException("Số lượng part không khớp: bản đã xếp có " + nestedParts.size()
                    + " part, bản chưa xếp có " + rawParts.size() + " part",
                    ErrorCodes.LAYOUT_PART_MISMATCH);
        }

        SvgFile svgFile = SvgFile.builder()
                .status("ACTIVE")
                .uploadedBy(currentUserService.getCurrentUser())
                .build();
        svgFile.setFileKey(uniqueFileKey(name));
        svgFile.setDisplayName(name.trim());
        svgFile.setFileCategory(category);
        svgFile.setModelYear(year);
        svgFile.setSource("SYSTEM");
        applyCutArea(svgFile, cutAreaLengthMm, cutAreaWidthMm, false);

        if (hasNested) {
            String storedFilename = UUID.randomUUID() + ".svg";
            String filePath = fileStorageService.storeFile(nestedBytes, storedFilename);
            svgFile.setOriginalFilename(FileUtils.getCleanFilename(nestedFile.getOriginalFilename()));
            svgFile.setStoredFilename(storedFilename);
            svgFile.setFilePath(filePath);
            svgFile.setFileSize((long) nestedBytes.length);
            svgFile.setContentType("image/svg+xml");
            svgFile.setChecksum(ChecksumUtils.calculateSha256(nestedBytes));
            svgFile.setFilmUsage(totalFilmUsage(nestedParts));
            addParts(svgFile, nestedParts, "NESTED");
        }

        if (hasRaw) {
            String rawStored = UUID.randomUUID() + ".svg";
            String rawPath = fileStorageService.storeFile(rawBytes, rawStored);
            svgFile.setRawOriginalFilename(FileUtils.getCleanFilename(rawFile.getOriginalFilename()));
            svgFile.setRawStoredFilename(rawStored);
            svgFile.setRawFilePath(rawPath);
            svgFile.setRawFileSize((long) rawBytes.length);
            svgFile.setRawChecksum(ChecksumUtils.calculateSha256(rawBytes));
            if (!hasNested) {
                svgFile.setFilmUsage(totalFilmUsage(rawParts));
            }
            addParts(svgFile, rawParts, "RAW");
        }

        for (VehicleNode node : nodes) {
            svgFile.getVehicleNodes().add(new SvgFileVehicleNode(svgFile, node));
        }

        storeThumbnailIfAny(thumbnail, svgFile);
        SvgFile saved = svgFileRepository.save(svgFile);
        String savedOrigName = saved.getOriginalFilename() != null ? saved.getOriginalFilename() : saved.getRawOriginalFilename();
        audit("UPLOAD_PART_FILE", saved, "Upload file thiết kế: " + savedOrigName);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AdminFileResponse updateFile(Long id, MultipartFile nestedFile, MultipartFile rawFile,
                                        boolean removeNested, boolean removeRaw,
                                        String name, Long categoryId,
                                        Integer year, boolean yearPresent,
                                        List<Long> vehicleNodeIds, MultipartFile thumbnail,
                                        Integer cutAreaLengthMm, Integer cutAreaWidthMm, boolean clearCutArea) {
        SvgFile svgFile = svgFileRepository.findById(id)
                .filter(f -> !"DELETED".equalsIgnoreCase(f.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file với ID: " + id));

        boolean currentHasNested = svgFile.hasNested();
        boolean currentHasRaw = svgFile.hasRaw();

        boolean newNestedProvided = nestedFile != null && !nestedFile.isEmpty();
        boolean newRawProvided = rawFile != null && !rawFile.isEmpty();

        boolean willHaveNested = (newNestedProvided || currentHasNested) && !removeNested;
        boolean willHaveRaw = (newRawProvided || currentHasRaw) && !removeRaw;

        if (!willHaveNested && !willHaveRaw) {
            throw new BadRequestException("Không thể bỏ cả hai bản file", ErrorCodes.FILE_REQUIRED);
        }

        byte[] newNestedBytes = null;
        List<ImportedPart> newNestedParts = null;
        if (newNestedProvided) {
            newNestedBytes = readAndValidateSvg(nestedFile);
            newNestedParts = SvgImport.parseParts(new String(newNestedBytes, java.nio.charset.StandardCharsets.UTF_8));
            if (newNestedParts.isEmpty()) {
                throw new BadRequestException("File SVG đã xếp không chứa hình kín nào để tách part",
                        ErrorCodes.UNSUPPORTED_FORMAT);
            }
        }

        byte[] newRawBytes = null;
        List<ImportedPart> newRawParts = null;
        if (newRawProvided) {
            newRawBytes = readAndValidateSvg(rawFile);
            newRawParts = SvgImport.parseParts(new String(newRawBytes, java.nio.charset.StandardCharsets.UTF_8));
            if (newRawParts.isEmpty()) {
                throw new BadRequestException("File SVG chưa xếp không chứa hình kín nào để tách part",
                        ErrorCodes.UNSUPPORTED_FORMAT);
            }
        }

        if (willHaveNested && willHaveRaw) {
            int nestedCount = newNestedProvided ? newNestedParts.size()
                    : (int) partRepository.countBySvgFileIdAndLayout(id, "NESTED");
            int rawCount = newRawProvided ? newRawParts.size()
                    : (int) partRepository.countBySvgFileIdAndLayout(id, "RAW");
            if (nestedCount != rawCount) {
                throw new BadRequestException("Số lượng part không khớp: bản đã xếp có " + nestedCount
                        + " part, bản chưa xếp có " + rawCount + " part",
                        ErrorCodes.LAYOUT_PART_MISMATCH);
            }
        }

        // Xử lý bản NESTED
        if (removeNested) {
            String oldPath = svgFile.getFilePath();
            svgFile.setOriginalFilename(null);
            svgFile.setStoredFilename(null);
            svgFile.setFilePath(null);
            svgFile.setFileSize(null);
            svgFile.setChecksum(null);
            svgFile.getParts().removeIf(p -> "NESTED".equalsIgnoreCase(p.getLayout()));
            svgFileRepository.flush();
            if (oldPath != null) {
                try {
                    fileStorageService.deleteFile(oldPath);
                } catch (Exception e) {
                    log.warn("Không xoá được file vật lý cũ {} của file ID {}", oldPath, id);
                }
            }
        } else if (newNestedProvided) {
            String oldPath = svgFile.getFilePath();
            String stored = UUID.randomUUID() + ".svg";
            String newPath = fileStorageService.storeFile(newNestedBytes, stored);
            svgFile.setOriginalFilename(FileUtils.getCleanFilename(nestedFile.getOriginalFilename()));
            svgFile.setStoredFilename(stored);
            svgFile.setFilePath(newPath);
            svgFile.setFileSize((long) newNestedBytes.length);
            svgFile.setContentType("image/svg+xml");
            svgFile.setChecksum(ChecksumUtils.calculateSha256(newNestedBytes));
            svgFile.getParts().removeIf(p -> "NESTED".equalsIgnoreCase(p.getLayout()));
            svgFileRepository.flush();
            addParts(svgFile, newNestedParts, "NESTED");
            if (oldPath != null) {
                try {
                    fileStorageService.deleteFile(oldPath);
                } catch (Exception e) {
                    log.warn("Không xoá được file vật lý cũ {} của file ID {}", oldPath, id);
                }
            }
        }

        // Xử lý bản RAW
        if (removeRaw) {
            String oldRawPath = svgFile.getRawFilePath();
            svgFile.setRawOriginalFilename(null);
            svgFile.setRawStoredFilename(null);
            svgFile.setRawFilePath(null);
            svgFile.setRawFileSize(null);
            svgFile.setRawChecksum(null);
            svgFile.getParts().removeIf(p -> "RAW".equalsIgnoreCase(p.getLayout()));
            svgFileRepository.flush();
            if (oldRawPath != null) {
                try {
                    fileStorageService.deleteFile(oldRawPath);
                } catch (Exception e) {
                    log.warn("Không xoá được file vật lý raw cũ {} của file ID {}", oldRawPath, id);
                }
            }
        } else if (newRawProvided) {
            String oldRawPath = svgFile.getRawFilePath();
            String rawStored = UUID.randomUUID() + ".svg";
            String newRawPath = fileStorageService.storeFile(newRawBytes, rawStored);
            svgFile.setRawOriginalFilename(FileUtils.getCleanFilename(rawFile.getOriginalFilename()));
            svgFile.setRawStoredFilename(rawStored);
            svgFile.setRawFilePath(newRawPath);
            svgFile.setRawFileSize((long) newRawBytes.length);
            svgFile.setRawChecksum(ChecksumUtils.calculateSha256(newRawBytes));
            svgFile.getParts().removeIf(p -> "RAW".equalsIgnoreCase(p.getLayout()));
            svgFileRepository.flush();
            addParts(svgFile, newRawParts, "RAW");
            if (oldRawPath != null) {
                try {
                    fileStorageService.deleteFile(oldRawPath);
                } catch (Exception e) {
                    log.warn("Không xoá được file vật lý raw cũ {} của file ID {}", oldRawPath, id);
                }
            }
        }

        // Cập nhật filmUsage theo bản đã xếp nếu có, không thì bản chưa xếp
        if (svgFile.hasNested()) {
            if (newNestedProvided) {
                svgFile.setFilmUsage(totalFilmUsage(newNestedParts));
            }
        } else if (svgFile.hasRaw()) {
            if (newRawProvided) {
                svgFile.setFilmUsage(totalFilmUsage(newRawParts));
            } else if (removeNested) {
                List<SvgFilePart> rawCurrent = svgFile.getParts().stream()
                        .filter(p -> "RAW".equalsIgnoreCase(p.getLayout())).toList();
                double mm = rawCurrent.stream()
                        .mapToDouble(p -> Math.max(p.getWidthMm() != null ? p.getWidthMm() : 0.0,
                                p.getHeightMm() != null ? p.getHeightMm() : 0.0)).sum();
                svgFile.setFilmUsage(formatFilmUsage(mm));
            }
        }

        if (name != null) {
            requireName(name);
            svgFile.setDisplayName(name.trim());
        }
        if (categoryId != null) {
            svgFile.setFileCategory(requireCategory(categoryId));
        }
        if (yearPresent) {
            validateYear(year);
            svgFile.setModelYear(year);
        }
        if (vehicleNodeIds != null) {
            List<VehicleNode> nodes = resolveVehicleNodes(vehicleNodeIds);
            svgFile.getVehicleNodes().clear();
            svgFileRepository.flush();
            for (VehicleNode node : nodes) {
                svgFile.getVehicleNodes().add(new SvgFileVehicleNode(svgFile, node));
            }
        }
        storeThumbnailIfAny(thumbnail, svgFile);
        applyCutArea(svgFile, cutAreaLengthMm, cutAreaWidthMm, clearCutArea);

        SvgFile saved = svgFileRepository.save(svgFile);
        audit("UPDATE_PART_FILE", saved, "Cập nhật file thiết kế ID: " + id);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteFile(Long id) {
        SvgFile svgFile = svgFileRepository.findById(id)
                .filter(f -> !"DELETED".equalsIgnoreCase(f.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file với ID: " + id));
        svgFile.setStatus("DELETED");
        svgFileRepository.save(svgFile);
        audit("DELETE_PART_FILE", svgFile, "Xoá mềm file thiết kế ID: " + id);
    }

    // ── nội bộ ──────────────────────────────────────────────────────────

    /**
     * Kiểm file là SVG ở CẢ đuôi lẫn nội dung (Q7): đuôi .svg, MIME hợp lệ, và nội
     * dung phải qua được bộ khử độc + gốc <svg>. .plt/.dxf hay file đổi đuôi → 400.
     */
    private byte[] readAndValidateSvg(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Thiếu file SVG để tải lên", ErrorCodes.UNSUPPORTED_FORMAT);
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("File '" + file.getOriginalFilename() + "' vượt quá dung lượng tối đa ("
                    + (maxFileSizeBytes / (1024 * 1024)) + " MB)");
        }
        String filename = FileUtils.getCleanFilename(file.getOriginalFilename());
        if (!FileUtils.isSvgExtension(filename)) {
            throw new BadRequestException("Chỉ chấp nhận file định dạng SVG (.svg): " + filename,
                    ErrorCodes.UNSUPPORTED_FORMAT);
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String lower = contentType.toLowerCase(Locale.ROOT);
            if (!lower.contains("svg") && !lower.contains("xml") && !lower.equals("application/octet-stream")) {
                throw new BadRequestException("Content-Type không hợp lệ cho file SVG: " + contentType,
                        ErrorCodes.UNSUPPORTED_FORMAT);
            }
        }
        byte[] raw;
        try {
            raw = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Không thể đọc nội dung file: " + filename);
        }
        // Nội dung phải là SVG thật — sanitizer ném InvalidSvgException khi không có gốc <svg>.
        return svgSanitizerService.sanitizeAndValidateSvg(raw);
    }

    /** Mỗi node gắn phải là MODEL hoặc SUBTYPE (§2 — file treo ở hai cấp lá xe). */
    private List<VehicleNode> resolveVehicleNodes(List<Long> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            throw new BadRequestException("Cần chọn một mẫu xe (vehicleNodeIds)");
        }
        List<Long> distinct = nodeIds.stream().distinct().toList();
        // Board 30/09 bỏ Q5: mỗi file gắn ĐÚNG một mẫu xe (Model hoặc Phiên bản).
        // V18 khoá thêm ở DB (UNIQUE svg_file_id) để không đường nào lách được.
        if (distinct.size() != 1) {
            throw new BadRequestException("Mỗi file chỉ gắn một mẫu xe — đang gửi " + distinct.size(),
                    ErrorCodes.ONE_VEHICLE_PER_FILE);
        }
        List<VehicleNode> nodes = new ArrayList<>();
        for (Long nodeId : distinct) {
            VehicleNode node = vehicleNodeRepository.findById(nodeId)
                    .orElseThrow(() -> new BadRequestException("Mẫu xe không tồn tại: " + nodeId));
            if (node.getLevel() != VehicleNodeLevel.MODEL && node.getLevel() != VehicleNodeLevel.SUBTYPE) {
                throw new BadRequestException("Chỉ gắn file vào MODEL hoặc SUBTYPE — node "
                        + nodeId + " là " + node.getLevel());
            }
            nodes.add(node);
        }
        return nodes;
    }

    /** Thêm parts của file cho một layout cụ thể (NESTED hoặc RAW) — SA §4/§8. */
    private void addParts(SvgFile svgFile, List<ImportedPart> parts, String layout) {
        Set<String> usedKeys = new HashSet<>();
        int order = 1;
        for (ImportedPart p : parts) {
            SvgFilePart part = new SvgFilePart();
            part.setSvgFile(svgFile);
            part.setLayout(layout);
            part.setPartKey(uniquePartKey(p.name(), usedKeys));
            part.setName(p.name());
            part.setDisplayOrder(order++);
            part.setPathData(p.pathData());
            part.setWidthMm(p.widthMm());
            part.setHeightMm(p.heightMm());
            part.setXMm(p.xMm());
            part.setYMm(p.yMm());
            part.setNodeCount(p.nodeCount());
            part.setHoleCount(p.holeCount());
            // film_usage tính từ hộp bao (§4): chiều dài phim ≈ cạnh dài hơn của part.
            part.setFilmUsage(formatFilmUsage(Math.max(p.widthMm(), p.heightMm())));
            svgFile.getParts().add(part);
        }
    }

    private String uniquePartKey(String name, Set<String> used) {
        String base = SlugUtils.slugify(name);
        if (base.isEmpty()) {
            base = "part";
        }
        String key = base;
        int n = 2;
        while (!used.add(key)) {
            key = base + "-" + n++;
        }
        return key;
    }

    /** file_key: slug tên + hậu tố ngắn ngẫu nhiên — duy nhất, ổn định qua các lần PUT. */
    private String uniqueFileKey(String name) {
        String base = SlugUtils.slugify(name);
        if (base.isEmpty()) {
            base = "file";
        }
        String key;
        do {
            key = base + "--" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        } while (svgFileRepository.existsByFileKey(key));
        return key;
    }

    /**
     * film_usage của cả file = tổng chiều dài phim các part (mỗi part = cạnh dài hơn
     * của hộp bao, §4). Định dạng "x,xx m" theo mẫu '6,46 m' của hợp đồng. ⚠ Đại
     * lượng đo filmUsage thật chờ câu A6a — con số này là ước lượng từ hộp bao.
     */
    private String totalFilmUsage(List<ImportedPart> parts) {
        double mm = parts.stream().mapToDouble(p -> Math.max(p.widthMm(), p.heightMm())).sum();
        return formatFilmUsage(mm);
    }

    private static String formatFilmUsage(double mm) {
        DecimalFormat df = new DecimalFormat("0.00",
                DecimalFormatSymbols.getInstance(new Locale("vi", "VN")));
        return df.format(mm / 1000.0) + " m";
    }

    private void storeThumbnailIfAny(MultipartFile thumbnail, SvgFile svgFile) {
        if (thumbnail == null || thumbnail.isEmpty()) {
            return;
        }
        String contentType = thumbnail.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BadRequestException("Thumbnail phải là file ảnh: " + contentType);
        }
        String ext = FileUtils.getFileExtension(FileUtils.getCleanFilename(thumbnail.getOriginalFilename()));
        String stored = UUID.randomUUID() + (ext.isEmpty() ? ".png" : "." + ext);
        try {
            svgFile.setThumbnailPath(fileStorageService.storeFile(thumbnail.getBytes(), stored));
        } catch (IOException e) {
            throw new BadRequestException("Không thể đọc nội dung thumbnail");
        }
    }

    private void requireName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BadRequestException("Thiếu tên hiển thị của file (name)");
        }
    }

    private FileCategory requireCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BadRequestException("Thiếu danh mục file (categoryId)");
        }
        return fileCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BadRequestException("Danh mục file không tồn tại: " + categoryId));
    }

    /**
     * Khổ cắt theo file (epic NGO-399): chỉ một trong hai → 400 CUT_AREA_INCOMPLETE;
     * ngoài giới hạn (dài 100–50000, rộng 100–2000 mm) → 400 CUT_AREA_OUT_OF_RANGE.
     * {@code clear}=true bỏ khổ đã khai; cả hai null (và không clear) → giữ nguyên.
     */
    private void applyCutArea(SvgFile file, Integer lengthMm, Integer widthMm, boolean clear) {
        if (clear) {
            file.setCutAreaLengthMm(null);
            file.setCutAreaWidthMm(null);
            return;
        }
        if (lengthMm == null && widthMm == null) {
            return;
        }
        if (lengthMm == null || widthMm == null) {
            throw new BadRequestException(
                    "Khổ cắt cần đủ cả hai trường cutAreaLengthMm và cutAreaWidthMm",
                    ErrorCodes.CUT_AREA_INCOMPLETE);
        }
        if (lengthMm < 100 || lengthMm > 50000 || widthMm < 100 || widthMm > 2000) {
            throw new BadRequestException(
                    "Khổ cắt ngoài giới hạn: chiều dài 100–50000 mm, khổ phim 100–2000 mm",
                    ErrorCodes.CUT_AREA_OUT_OF_RANGE);
        }
        file.setCutAreaLengthMm(lengthMm);
        file.setCutAreaWidthMm(widthMm);
    }

    private void validateYear(Integer year) {
        if (year != null && (year < 1900 || year > 2100)) {
            throw new BadRequestException("Năm xe phải trong 1900..2100: " + year);
        }
    }

    private AdminFileResponse toResponse(SvgFile f) {
        List<AdminFileResponse.VehicleRef> vehicles = f.getVehicleNodes().stream()
                .map(l -> new AdminFileResponse.VehicleRef(
                        l.getVehicleNode().getId(), vehiclePath(l.getVehicleNode())))
                .toList();

        boolean hasNested = f.hasNested();
        boolean hasRaw = f.hasRaw();

        int partCount = 0;
        if (f.getId() == null) {
            if (hasNested) {
                partCount = (int) f.getParts().stream().filter(p -> "NESTED".equalsIgnoreCase(p.getLayout())).count();
            } else if (hasRaw) {
                partCount = (int) f.getParts().stream().filter(p -> "RAW".equalsIgnoreCase(p.getLayout())).count();
            }
        } else {
            if (hasNested) {
                partCount = (int) partRepository.countBySvgFileIdAndLayout(f.getId(), "NESTED");
            } else if (hasRaw) {
                partCount = (int) partRepository.countBySvgFileIdAndLayout(f.getId(), "RAW");
            }
        }

        String origName = f.getOriginalFilename() != null ? f.getOriginalFilename() : f.getRawOriginalFilename();
        String displayName = f.getDisplayName() != null ? f.getDisplayName() : origName;

        return new AdminFileResponse(
                f.getId(), f.getFileKey(),
                displayName,
                origName,
                f.getFileCategory() != null ? f.getFileCategory().getName() : null,
                f.getModelYear(), vehicles, f.getSource(), partCount, f.getUpdatedAt(),
                f.getThumbnailPath() != null ? "/api/svg/" + f.getId() + "/thumbnail" : null,
                hasNested,
                hasRaw,
                f.getCutAreaLengthMm(),
                f.getCutAreaWidthMm());
    }

    /** Đường dẫn tên từ gốc tới node: "Toyota › Camry › Camry 2.5Q". */
    private String vehiclePath(VehicleNode node) {
        List<String> names = new ArrayList<>();
        for (VehicleNode n = node; n != null; n = n.getParent()) {
            names.add(0, n.getName());
        }
        return String.join(" › ", names);
    }

    private void audit(String action, SvgFile file, String detail) {
        User u = currentUserService.getCurrentUser();
        auditLogService.log(u.getUsername(), u.getRole().name(), action, "SvgFile", file.getId(), detail);
    }
}

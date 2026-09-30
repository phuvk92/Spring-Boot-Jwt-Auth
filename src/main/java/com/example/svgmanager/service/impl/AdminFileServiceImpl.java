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
    public AdminFileResponse createFile(MultipartFile file, String name, Long categoryId, Integer year,
                                        List<Long> vehicleNodeIds, MultipartFile thumbnail) {
        requireName(name);
        FileCategory category = requireCategory(categoryId);
        validateYear(year);
        List<VehicleNode> nodes = resolveVehicleNodes(vehicleNodeIds);

        byte[] svgBytes = readAndValidateSvg(file);
        // Tách part TRƯỚC khi lưu: file hỏng phải rớt 400, không được để lại bản ghi nửa vời.
        List<ImportedPart> parts = SvgImport.parseParts(new String(svgBytes, java.nio.charset.StandardCharsets.UTF_8));
        if (parts.isEmpty()) {
            throw new BadRequestException("File SVG không chứa hình kín nào để tách part",
                    ErrorCodes.UNSUPPORTED_FORMAT);
        }

        String storedFilename = UUID.randomUUID() + ".svg";
        String filePath = fileStorageService.storeFile(svgBytes, storedFilename);

        SvgFile svgFile = SvgFile.builder()
                .originalFilename(FileUtils.getCleanFilename(file.getOriginalFilename()))
                .storedFilename(storedFilename)
                .filePath(filePath)
                .fileSize((long) svgBytes.length)
                .contentType("image/svg+xml")
                .checksum(ChecksumUtils.calculateSha256(svgBytes))
                .status("ACTIVE")
                .uploadedBy(currentUserService.getCurrentUser())
                .build();
        svgFile.setFileKey(uniqueFileKey(name));
        svgFile.setDisplayName(name.trim());
        svgFile.setFileCategory(category);
        svgFile.setModelYear(year);
        svgFile.setSource("SYSTEM");
        svgFile.setFilmUsage(totalFilmUsage(parts));

        replaceParts(svgFile, parts);
        for (VehicleNode node : nodes) {
            svgFile.getVehicleNodes().add(new SvgFileVehicleNode(svgFile, node));
        }

        storeThumbnailIfAny(thumbnail, svgFile);
        SvgFile saved = svgFileRepository.save(svgFile);
        audit("UPLOAD_PART_FILE", saved, "Upload file thiết kế: " + saved.getOriginalFilename());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AdminFileResponse updateFile(Long id, MultipartFile file, String name, Long categoryId,
                                        Integer year, boolean yearPresent,
                                        List<Long> vehicleNodeIds, MultipartFile thumbnail) {
        SvgFile svgFile = svgFileRepository.findById(id)
                .filter(f -> !"DELETED".equalsIgnoreCase(f.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file với ID: " + id));

        if (file != null && !file.isEmpty()) {
            byte[] svgBytes = readAndValidateSvg(file);
            List<ImportedPart> parts = SvgImport.parseParts(
                    new String(svgBytes, java.nio.charset.StandardCharsets.UTF_8));
            if (parts.isEmpty()) {
                throw new BadRequestException("File SVG không chứa hình kín nào để tách part",
                        ErrorCodes.UNSUPPORTED_FORMAT);
            }
            String storedFilename = UUID.randomUUID() + ".svg";
            String newPath = fileStorageService.storeFile(svgBytes, storedFilename);

            String oldPath = svgFile.getFilePath();
            svgFile.setOriginalFilename(FileUtils.getCleanFilename(file.getOriginalFilename()));
            svgFile.setStoredFilename(storedFilename);
            svgFile.setFilePath(newPath);
            svgFile.setFileSize((long) svgBytes.length);
            svgFile.setChecksum(ChecksumUtils.calculateSha256(svgBytes));
            svgFile.setFilmUsage(totalFilmUsage(parts));
            replaceParts(svgFile, parts);
            try {
                fileStorageService.deleteFile(oldPath);
            } catch (Exception e) {
                log.warn("Không xoá được file vật lý cũ {} của file ID {}", oldPath, id);
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
            // Clear + flush xoá orphan trước — gắn lại cùng node ngay sau đó mà để chung
            // một flush thì insert chạy trước delete, đụng PK (svg_file_id, vehicle_node_id).
            svgFile.getVehicleNodes().clear();
            svgFileRepository.flush();
            for (VehicleNode node : nodes) {
                svgFile.getVehicleNodes().add(new SvgFileVehicleNode(svgFile, node));
            }
        }
        storeThumbnailIfAny(thumbnail, svgFile);

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

    /** Thay TOÀN BỘ parts của file — PUT có file mới là tách lại từ đầu (§3.2). */
    private void replaceParts(SvgFile svgFile, List<ImportedPart> parts) {
        // Flush xoá orphan TRƯỚC khi thêm part mới: Hibernate insert trước delete trong
        // cùng một flush, mà part_key mới có thể trùng part_key cũ → đụng UNIQUE
        // (svg_file_id, part_key). Clear + flush riêng là xoá xong hẳn mới insert.
        if (!svgFile.getParts().isEmpty()) {
            svgFile.getParts().clear();
            svgFileRepository.flush();
        }
        Set<String> usedKeys = new HashSet<>();
        int order = 1;
        for (ImportedPart p : parts) {
            SvgFilePart part = new SvgFilePart();
            part.setSvgFile(svgFile);
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
        int partCount = f.getId() == null ? f.getParts().size()
                : (int) partRepository.countBySvgFileId(f.getId());
        return new AdminFileResponse(
                f.getId(), f.getFileKey(),
                f.getDisplayName() != null ? f.getDisplayName() : f.getOriginalFilename(),
                f.getOriginalFilename(),
                f.getFileCategory() != null ? f.getFileCategory().getName() : null,
                f.getModelYear(), vehicles, f.getSource(), partCount, f.getUpdatedAt(),
                f.getThumbnailPath() != null ? "/api/svg/" + f.getId() + "/thumbnail" : null);
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

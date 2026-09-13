package com.example.svgmanager.service.impl;

import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.SvgResponse;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.BadRequestException;
import com.example.svgmanager.exception.ResourceNotFoundException;
import com.example.svgmanager.mapper.SvgMapper;
import com.example.svgmanager.repository.SvgFileRepository;
import com.example.svgmanager.repository.UserRepository;
import com.example.svgmanager.service.FileStorageService;
import com.example.svgmanager.service.SvgSanitizerService;
import com.example.svgmanager.service.SvgService;
import com.example.svgmanager.util.ChecksumUtils;
import com.example.svgmanager.util.FileUtils;
import com.example.svgmanager.util.SecurityUtils;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SvgServiceImpl implements SvgService {

    private static final Logger log = LoggerFactory.getLogger(SvgServiceImpl.class);

    private final SvgFileRepository svgFileRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final SvgSanitizerService svgSanitizerService;
    private final SvgMapper svgMapper;
    private final long maxFileSizeBytes;

    public SvgServiceImpl(
            SvgFileRepository svgFileRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            SvgSanitizerService svgSanitizerService,
            SvgMapper svgMapper,
            @Value("${app.file.max-file-size-bytes:10485760}") long maxFileSizeBytes
    ) {
        this.svgFileRepository = svgFileRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.svgSanitizerService = svgSanitizerService;
        this.svgMapper = svgMapper;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    @Transactional
    public SvgResponse uploadSvg(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("File size exceeds the allowed limit of " + (maxFileSizeBytes / (1024 * 1024)) + "MB");
        }

        String rawOriginalFilename = file.getOriginalFilename();
        String safeOriginalFilename = FileUtils.getCleanFilename(rawOriginalFilename);

        if (!FileUtils.isSvgExtension(safeOriginalFilename)) {
            throw new BadRequestException("Only SVG files are allowed (.svg)");
        }

        // Validate content-type if provided
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String lowerType = contentType.toLowerCase();
            if (!lowerType.contains("svg") && !lowerType.contains("xml") && !lowerType.contains("octet-stream")) {
                throw new BadRequestException("Invalid content type for SVG: " + contentType);
            }
        }

        byte[] rawBytes;
        try {
            rawBytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded file content");
        }

        // Sanitize and validate SVG content against XSS / XXE
        byte[] sanitizedBytes = svgSanitizerService.sanitizeAndValidateSvg(rawBytes);

        // Generate safe unique stored filename
        String storedFilename = UUID.randomUUID().toString() + ".svg";

        // Store physical file
        String filePath = fileStorageService.storeFile(sanitizedBytes, storedFilename);

        // Compute checksum
        String checksum = ChecksumUtils.calculateSha256(sanitizedBytes);

        // Get currently authenticated user
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        SvgFile svgFile = SvgFile.builder()
                .originalFilename(safeOriginalFilename)
                .storedFilename(storedFilename)
                .filePath(filePath)
                .fileSize((long) sanitizedBytes.length)
                .contentType("image/svg+xml")
                .checksum(checksum)
                .uploadedBy(currentUser)
                .build();

        SvgFile saved = svgFileRepository.save(svgFile);
        log.info("[SVG_UPLOADED] SVG file uploaded: id={}, originalName='{}', storedName='{}', uploadedBy='{}'",
                saved.getId(), saved.getOriginalFilename(), saved.getStoredFilename(), currentUser.getUsername());

        return svgMapper.toSvgResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SvgResponse> getSvgFiles(
            String keyword,
            Long uploadedBy,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String validSortBy = StringUtils.hasText(sortBy) ? sortBy : "createdAt";
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, validSortBy));

        Specification<SvgFile> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(cb.lower(root.get("originalFilename")), "%" + keyword.toLowerCase() + "%"));
            }
            if (uploadedBy != null) {
                predicates.add(cb.equal(root.get("uploadedBy").get("id"), uploadedBy));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SvgFile> pageResult = svgFileRepository.findAll(spec, pageable);
        return PageResponse.of(pageResult.map(svgMapper::toSvgResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public SvgResponse getSvgFileById(Long id) {
        SvgFile svgFile = svgFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SVG file not found with id: " + id));
        return svgMapper.toSvgResponse(svgFile);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource previewSvg(Long id) {
        SvgFile svgFile = svgFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SVG file not found with id: " + id));

        Resource resource = fileStorageService.loadFileAsResource(svgFile.getFilePath());
        log.info("[SVG_PREVIEWED] SVG file previewed: id={}, originalName='{}'", id, svgFile.getOriginalFilename());
        return resource;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadSvg(Long id) {
        SvgFile svgFile = svgFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SVG file not found with id: " + id));

        Resource resource = fileStorageService.loadFileAsResource(svgFile.getFilePath());
        log.info("[SVG_DOWNLOADED] SVG file downloaded: id={}, originalName='{}'", id, svgFile.getOriginalFilename());
        return resource;
    }

    @Override
    @Transactional(readOnly = true)
    public String getOriginalFilename(Long id) {
        return svgFileRepository.findById(id)
                .map(SvgFile::getOriginalFilename)
                .orElseThrow(() -> new ResourceNotFoundException("SVG file not found with id: " + id));
    }

    @Override
    @Transactional
    public void deleteSvg(Long id) {
        SvgFile svgFile = svgFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SVG file not found with id: " + id));

        // Delete physical file
        fileStorageService.deleteFile(svgFile.getFilePath());

        // Delete metadata in DB
        svgFileRepository.delete(svgFile);
        log.info("[SVG_DELETED] SVG file deleted: id={}, originalName='{}'", id, svgFile.getOriginalFilename());
    }
}

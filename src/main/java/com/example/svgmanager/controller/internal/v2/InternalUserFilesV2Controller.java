package com.example.svgmanager.controller.internal.v2;

import com.example.svgmanager.dto.internal.v2.*;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.internal.v2.InternalUserFileV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/internal/v2/user-files")
@Tag(
        name = "Internal User Files V2",
        description = "Encrypted SVG file APIs V2 — Hoàn toàn độc lập với V1, tích hợp mã hoá AES-256-GCM và bảo mật SVG đa lớp"
)
@SecurityRequirement(name = "bearerAuth")
public class InternalUserFilesV2Controller {

    private final InternalUserFileV2Service userFileV2Service;
    private final CurrentUserService currentUserService;

    public InternalUserFilesV2Controller(
            InternalUserFileV2Service userFileV2Service,
            CurrentUserService currentUserService
    ) {
        this.userFileV2Service = userFileV2Service;
        this.currentUserService = currentUserService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Lưu file SVG V2 có mã hoá AES-256-GCM (Multipart)",
            description = "Người dùng máy cắt lưu bản file SVG. Tự động kiểm tra bảo mật XML/SVG và mã hoá AES-256-GCM trước khi lưu vào V2 storage.")
    public ResponseEntity<UserSvgFileV2Response> saveUserFileMultipart(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "vehicleNodeId", required = false) Long vehicleNodeId,
            @RequestParam(value = "vehicleConfigurationId", required = false) Long vehicleConfigurationId,
            @RequestParam(value = "brandName", required = false) String brandName,
            @RequestParam(value = "modelName", required = false) String modelName,
            @RequestParam(value = "yearFrom", required = false) Integer yearFrom,
            @RequestParam(value = "yearTo", required = false) Integer yearTo,
            @RequestParam(value = "generationCode", required = false) String generationCode,
            @RequestParam(value = "productGroup", required = false) String productGroup,
            @RequestParam(value = "productGroupName", required = false) String productGroupName,
            @RequestParam(value = "filmWidth", required = false) Double filmWidth,
            @RequestParam(value = "filmWidthUnit", required = false) String filmWidthUnit,
            @RequestParam(value = "rollLength", required = false) Double rollLength,
            @RequestParam(value = "rollLengthUnit", required = false) String rollLengthUnit,
            @RequestParam(value = "axisX", required = false) Double axisX,
            @RequestParam(value = "axisY", required = false) Double axisY,
            @RequestParam(value = "sourceFileKey", required = false) String sourceFileKey,
            @RequestParam(value = "description", required = false) String description
    ) throws IOException {
        User currentUser = currentUserService.getCurrentUser();
        Long effectiveNodeId = vehicleNodeId != null ? vehicleNodeId : vehicleConfigurationId;

        UserSvgFileV2Response response = userFileV2Service.saveUserFile(
                currentUser,
                file.getBytes(),
                file.getOriginalFilename(),
                file.getContentType(),
                fileName,
                categoryId,
                effectiveNodeId,
                brandName,
                modelName,
                yearFrom,
                yearTo,
                generationCode,
                productGroup,
                productGroupName,
                filmWidth,
                filmWidthUnit,
                rollLength,
                rollLengthUnit,
                axisX,
                axisY,
                sourceFileKey,
                description
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lưu file SVG V2 có mã hoá AES-256-GCM (JSON)",
            description = "Người dùng máy cắt lưu bản file SVG dạng chuỗi SVG trong body JSON. Tự động kiểm tra bảo mật XML/SVG và mã hoá AES-256-GCM.")
    public ResponseEntity<UserSvgFileV2Response> saveUserFileJson(
            @RequestBody UserSvgFileV2UploadRequest body
    ) {
        User currentUser = currentUserService.getCurrentUser();
        byte[] bytes = body.getSvgContent() != null
                ? body.getSvgContent().getBytes(StandardCharsets.UTF_8)
                : new byte[0];

        UserSvgFileV2Response response = userFileV2Service.saveUserFile(
                currentUser,
                bytes,
                body.getFileName(),
                "image/svg+xml",
                body.getFileName(),
                body.getCategoryId(),
                body.getVehicleNodeId(),
                body.getBrandName(),
                body.getModelName(),
                body.getYearFrom(),
                body.getYearTo(),
                body.getGenerationCode(),
                body.getProductGroup(),
                body.getProductGroupName(),
                body.getFilmWidth(),
                body.getFilmWidthUnit(),
                body.getRollLength(),
                body.getRollLengthUnit(),
                body.getAxisX(),
                body.getAxisY(),
                body.getSourceFileKey(),
                body.getDescription()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ghi đè file SVG V2 của chính người dùng (Multipart)")
    public ResponseEntity<UserSvgFileV2Response> updateUserFileMultipart(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "vehicleNodeId", required = false) Long vehicleNodeId,
            @RequestParam(value = "vehicleConfigurationId", required = false) Long vehicleConfigurationId,
            @RequestParam(value = "brandName", required = false) String brandName,
            @RequestParam(value = "modelName", required = false) String modelName,
            @RequestParam(value = "yearFrom", required = false) Integer yearFrom,
            @RequestParam(value = "yearTo", required = false) Integer yearTo,
            @RequestParam(value = "generationCode", required = false) String generationCode,
            @RequestParam(value = "productGroup", required = false) String productGroup,
            @RequestParam(value = "productGroupName", required = false) String productGroupName,
            @RequestParam(value = "filmWidth", required = false) Double filmWidth,
            @RequestParam(value = "filmWidthUnit", required = false) String filmWidthUnit,
            @RequestParam(value = "rollLength", required = false) Double rollLength,
            @RequestParam(value = "rollLengthUnit", required = false) String rollLengthUnit,
            @RequestParam(value = "axisX", required = false) Double axisX,
            @RequestParam(value = "axisY", required = false) Double axisY,
            @RequestParam(value = "sourceFileKey", required = false) String sourceFileKey,
            @RequestParam(value = "description", required = false) String description
    ) throws IOException {
        User currentUser = currentUserService.getCurrentUser();
        Long effectiveNodeId = vehicleNodeId != null ? vehicleNodeId : vehicleConfigurationId;
        byte[] bytes = file != null ? file.getBytes() : null;
        String orig = file != null ? file.getOriginalFilename() : null;
        String mime = file != null ? file.getContentType() : "image/svg+xml";

        UserSvgFileV2Response response = userFileV2Service.updateUserFile(
                currentUser,
                id,
                bytes,
                orig,
                mime,
                fileName,
                categoryId,
                effectiveNodeId,
                brandName,
                modelName,
                yearFrom,
                yearTo,
                generationCode,
                productGroup,
                productGroupName,
                filmWidth,
                filmWidthUnit,
                rollLength,
                rollLengthUnit,
                axisX,
                axisY,
                sourceFileKey,
                description
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Ghi đè file SVG V2 của chính người dùng (JSON)")
    public ResponseEntity<UserSvgFileV2Response> updateUserFileJson(
            @PathVariable Long id,
            @RequestBody UserSvgFileV2UploadRequest body
    ) {
        User currentUser = currentUserService.getCurrentUser();
        byte[] bytes = body.getSvgContent() != null
                ? body.getSvgContent().getBytes(StandardCharsets.UTF_8)
                : null;

        UserSvgFileV2Response response = userFileV2Service.updateUserFile(
                currentUser,
                id,
                bytes,
                body.getFileName(),
                "image/svg+xml",
                body.getFileName(),
                body.getCategoryId(),
                body.getVehicleNodeId(),
                body.getBrandName(),
                body.getModelName(),
                body.getYearFrom(),
                body.getYearTo(),
                body.getGenerationCode(),
                body.getProductGroup(),
                body.getProductGroupName(),
                body.getFilmWidth(),
                body.getFilmWidthUnit(),
                body.getRollLength(),
                body.getRollLengthUnit(),
                body.getAxisX(),
                body.getAxisY(),
                body.getSourceFileKey(),
                body.getDescription()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Xem danh sách bản lưu SVG V2 của người dùng và các bản được chia sẻ",
            description = "Trả về danh sách file SVG V2 (accessType=OWNER hoặc SHARED).")
    public ResponseEntity<PageResponse<UserSvgFileV2Response>> listMyFiles(
            @Parameter(description = "Từ khoá tìm kiếm") @RequestParam(required = false) String keyword,
            @Parameter(description = "Danh mục") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Trạng thái") @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User currentUser = currentUserService.getCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<UserSvgFileV2Response> response = userFileV2Service.listUserFiles(
                currentUser, keyword, categoryId, status, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một bản lưu SVG V2")
    public ResponseEntity<UserSvgFileV2Response> getMyFileDetail(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userFileV2Service.getUserFile(currentUser, id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Tải file SVG V2 đã giải mã",
            description = "Hệ thống tự động giải mã AES-256-GCM từ V2 storage và trả về file gốc .svg an toàn trong suốt với client.")
    public ResponseEntity<Resource> downloadMyFile(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        Resource resource = userFileV2Service.downloadUserFile(currentUser, id);
        String filename = userFileV2Service.getOriginalFilename(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/svg+xml;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("Cache-Control", "private, no-store");

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Xem trước file SVG V2 (inline stream đã giải mã)",
            description = "Giải mã on-the-fly và stream nội dung image/svg+xml kèm security headers nghiêm ngặt (CSP, nosniff, private cache).")
    public ResponseEntity<byte[]> previewMyFile(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        byte[] bytes = userFileV2Service.getUserFileBytes(currentUser, id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/svg+xml;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.inline().build());
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("Content-Security-Policy", "default-src 'none'; img-src 'self' data:;");
        headers.add("Cache-Control", "private, no-store");

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá mềm một bản lưu SVG V2 của chính người dùng",
            description = "Chỉ owner hoặc Admin mới có quyền xoá. Đặt status = DELETED. Trả về 204.")
    public ResponseEntity<Void> deleteMyFile(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        userFileV2Service.deleteUserFile(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{fileId}/shares")
    @Operation(summary = "Chia sẻ file SVG V2 cho người dùng khác",
            description = "Chỉ owner của file hoặc ADMIN mới có quyền chia sẻ file này. Người được chia sẻ chỉ có quyền xem/tải.")
    public ResponseEntity<UserSvgFileV2ShareResponse> shareFile(
            @PathVariable Long fileId,
            @Valid @RequestBody UserSvgFileV2ShareRequest request
    ) {
        User currentUser = currentUserService.getCurrentUser();
        UserSvgFileV2ShareResponse response = userFileV2Service.shareFile(currentUser, fileId, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{fileId}/shares")
    @Operation(summary = "Lấy danh sách người dùng được chia sẻ file SVG V2",
            description = "Chỉ owner của file hoặc ADMIN mới có quyền xem danh sách chia sẻ.")
    public ResponseEntity<FileSharesV2Response> getFileShares(@PathVariable Long fileId) {
        User currentUser = currentUserService.getCurrentUser();
        FileSharesV2Response response = userFileV2Service.getShares(currentUser, fileId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{fileId}/shares/{targetUserId}")
    @Operation(summary = "Thu hồi quyền chia sẻ file SVG V2",
            description = "Chỉ owner của file hoặc ADMIN mới có quyền thu hồi chia sẻ.")
    public ResponseEntity<Void> revokeFileShare(
            @PathVariable Long fileId,
            @PathVariable Long targetUserId
    ) {
        User currentUser = currentUserService.getCurrentUser();
        userFileV2Service.revokeShare(currentUser, fileId, targetUserId);
        return ResponseEntity.noContent().build();
    }
}

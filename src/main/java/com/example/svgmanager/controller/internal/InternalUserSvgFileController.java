package com.example.svgmanager.controller.internal;

import com.example.svgmanager.dto.request.ShareFileRequest;
import com.example.svgmanager.dto.request.UserSavedFileCreateRequest;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.FileSharesResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.UserSavedFileResponse;
import com.example.svgmanager.dto.response.UserSvgFileShareResponse;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.UserSvgFileService;
import com.example.svgmanager.service.UserSvgFileShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/internal/user-files")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Internal User Files", description = "Endpoints lưu và quản lý file SVG của người dùng máy cắt")
@PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
public class InternalUserSvgFileController {

    private final UserSvgFileService userSvgFileService;
    private final UserSvgFileShareService userSvgFileShareService;
    private final CurrentUserService currentUserService;

    public InternalUserSvgFileController(UserSvgFileService userSvgFileService,
                                         UserSvgFileShareService userSvgFileShareService,
                                         CurrentUserService currentUserService) {
        this.userSvgFileService = userSvgFileService;
        this.userSvgFileShareService = userSvgFileShareService;
        this.currentUserService = currentUserService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Lưu file SVG của người dùng (Multipart)",
            description = "Người dùng máy cắt lưu bản file SVG đã chỉnh sửa / xếp part.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lưu file SVG thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserSavedFileResponse> saveUserFileMultipart(
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
            @RequestParam(value = "filmWidthUnit", required = false, defaultValue = "MM") String filmWidthUnit,
            @RequestParam(value = "rollLength", required = false) Double rollLength,
            @RequestParam(value = "rollLengthUnit", required = false, defaultValue = "MM") String rollLengthUnit,
            @RequestParam(value = "axisX", required = false) Double axisX,
            @RequestParam(value = "axisY", required = false) Double axisY,
            @RequestParam(value = "sourceFileKey", required = false) String sourceFileKey,
            @RequestParam(value = "description", required = false) String description
    ) throws IOException {
        User currentUser = currentUserService.getCurrentUser();
        Long effectiveNodeId = vehicleNodeId != null ? vehicleNodeId : vehicleConfigurationId;

        UserSavedFileResponse response = userSvgFileService.saveUserFile(
                currentUser,
                file.getBytes(),
                file.getOriginalFilename(),
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
    @Operation(summary = "Lưu file SVG của người dùng (JSON)",
            description = "Người dùng máy cắt lưu bản file SVG dạng chuỗi SVG trong body JSON.")
    public ResponseEntity<UserSavedFileResponse> saveUserFileJson(
            @RequestBody UserSavedFileCreateRequest body
    ) {
        User currentUser = currentUserService.getCurrentUser();
        byte[] bytes = body.getSvgContent() != null
                ? body.getSvgContent().getBytes(StandardCharsets.UTF_8)
                : new byte[0];

        UserSavedFileResponse response = userSvgFileService.saveUserFile(
                currentUser,
                bytes,
                body.getFileName(),
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
    @Operation(summary = "Lưu đè file SVG của chính người dùng (Multipart)",
            description = "Người dùng máy cắt ghi đè nội dung SVG và cập nhật thông tin của bản lưu cùng ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cập nhật bản lưu thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bản lưu hoặc không thuộc người dùng", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserSavedFileResponse> updateUserFileMultipart(
            @PathVariable Long id,
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

        UserSavedFileResponse response = userSvgFileService.updateUserFile(
                currentUser,
                id,
                file.getBytes(),
                file.getOriginalFilename(),
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
    @Operation(summary = "Lưu đè file SVG của chính người dùng (JSON)",
            description = "Người dùng máy cắt ghi đè nội dung SVG và cập nhật thông tin của bản lưu cùng ID qua JSON.")
    public ResponseEntity<UserSavedFileResponse> updateUserFileJson(
            @PathVariable Long id,
            @RequestBody UserSavedFileCreateRequest body
    ) {
        User currentUser = currentUserService.getCurrentUser();
        byte[] bytes = body.getSvgContent() != null
                ? body.getSvgContent().getBytes(StandardCharsets.UTF_8)
                : new byte[0];

        UserSavedFileResponse response = userSvgFileService.updateUserFile(
                currentUser,
                id,
                bytes,
                body.getFileName(),
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
    @Operation(summary = "Xem danh sách bản lưu SVG của chính người dùng và các bản được chia sẻ",
            description = "Trả về các file SVG do chính user lưu (accessType=OWNER) hoặc được người khác chia sẻ (accessType=SHARED).")
    public ResponseEntity<PageResponse<UserSavedFileResponse>> listMyFiles(
            @Parameter(description = "Từ khoá tìm kiếm") @RequestParam(required = false) String keyword,
            @Parameter(description = "Danh mục") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Trạng thái") @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User currentUser = currentUserService.getCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<UserSavedFileResponse> response = userSvgFileService.listUserFiles(
                currentUser, keyword, categoryId, status, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một bản lưu SVG của người dùng hoặc bản được chia sẻ")
    public ResponseEntity<UserSavedFileResponse> getMyFileDetail(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userSvgFileService.getUserFile(currentUser, id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Tải file SVG đã lưu của người dùng hoặc bản được chia sẻ")
    public ResponseEntity<Resource> downloadMyFile(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        Resource resource = userSvgFileService.downloadUserFile(currentUser, id);
        String filename = userSvgFileService.getOriginalFilename(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/svg+xml"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Xem trước file SVG (inline stream)")
    public ResponseEntity<byte[]> previewMyFile(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        byte[] bytes = userSvgFileService.getUserFileBytes(currentUser, id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/svg+xml;charset=UTF-8"));
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá mềm một bản lưu SVG của chính người dùng",
            description = "Chỉ xoá bản thuộc chính user. Đặt status = DELETED, file SVG trên đĩa giữ nguyên. Trả 204.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Xoá bản lưu thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bản lưu hoặc không thuộc người dùng", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteMyFile(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        userSvgFileService.deleteUserFile(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/shares")
    @Operation(summary = "Chia sẻ file SVG cho người dùng khác",
            description = "Owner của file chia sẻ quyền xem/tải file SVG cho một USER khác.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chia sẻ file thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc tự chia sẻ cho chính mình", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền chia sẻ file này", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy file hoặc người nhận", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserSvgFileShareResponse> shareFile(
            @PathVariable Long id,
            @Valid @RequestBody ShareFileRequest request
    ) {
        User currentUser = currentUserService.getCurrentUser();
        UserSvgFileShareResponse response = userSvgFileShareService.shareFile(currentUser, id, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/shares")
    @Operation(summary = "Lấy danh sách người dùng được chia sẻ file SVG",
            description = "Chỉ owner hoặc ADMIN được phép xem danh sách người được chia sẻ file.")
    public ResponseEntity<FileSharesResponse> getFileShares(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userSvgFileShareService.getShares(currentUser, id));
    }

    @DeleteMapping("/{id}/shares/{targetUserId}")
    @Operation(summary = "Thu hồi quyền chia sẻ file SVG",
            description = "Chỉ owner hoặc ADMIN được phép thu hồi quyền chia sẻ.")
    public ResponseEntity<Void> revokeFileShare(
            @PathVariable Long id,
            @PathVariable Long targetUserId
    ) {
        User currentUser = currentUserService.getCurrentUser();
        userSvgFileShareService.revokeShare(currentUser, id, targetUserId);
        return ResponseEntity.noContent().build();
    }
}

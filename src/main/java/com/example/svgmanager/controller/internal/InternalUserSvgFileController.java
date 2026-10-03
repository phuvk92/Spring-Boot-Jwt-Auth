package com.example.svgmanager.controller.internal;

import com.example.svgmanager.dto.request.UserSavedFileCreateRequest;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.UserSavedFileResponse;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.UserSvgFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    private final CurrentUserService currentUserService;

    public InternalUserSvgFileController(UserSvgFileService userSvgFileService,
                                         CurrentUserService currentUserService) {
        this.userSvgFileService = userSvgFileService;
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
                body.getDescription()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Xem danh sách bản lưu SVG của chính người dùng",
            description = "Chỉ trả về các file SVG do chính user hiện tại lưu.")
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
    @Operation(summary = "Xem chi tiết một bản lưu SVG của chính người dùng")
    public ResponseEntity<UserSavedFileResponse> getMyFileDetail(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userSvgFileService.getUserFile(currentUser, id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Tải file SVG đã lưu của chính người dùng")
    public ResponseEntity<Resource> downloadMyFile(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        Resource resource = userSvgFileService.downloadUserFile(currentUser, id);
        String filename = userSvgFileService.getOriginalFilename(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/svg+xml"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());

        return ResponseEntity.ok().headers(headers).body(resource);
    }
}

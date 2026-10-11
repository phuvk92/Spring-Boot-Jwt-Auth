package com.example.svgmanager.controller;

import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.UserSavedFileResponse;
import com.example.svgmanager.entity.Role;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.exception.ForbiddenException;
import com.example.svgmanager.repository.UserRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;

@RestController
@RequestMapping("/api/agent/user-files")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Agent User Saved Files", description = "Đại lý xem danh sách và chi tiết metadata bản lưu của các User thuộc đại lý mình quản lý (chỉ đọc)")
@PreAuthorize("hasRole('AGENT')")
public class AgentUserSvgFileController {

    private final UserSvgFileService userSvgFileService;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @Autowired
    public AgentUserSvgFileController(UserSvgFileService userSvgFileService,
                                     CurrentUserService currentUserService,
                                     UserRepository userRepository) {
        this.userSvgFileService = userSvgFileService;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Xem danh sách bản SVG đã lưu của người dùng thuộc đại lý",
            description = "AGENT chỉ được quyền xem các bản lưu của user thuộc đúng đại lý của mình. Chỉ đọc metadata.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách bản lưu SVG phân trang"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền AGENT", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<UserSavedFileResponse>> listAgentFiles(
            @Parameter(description = "Từ khoá tìm kiếm") @RequestParam(required = false) String keyword,
            @Parameter(description = "Lọc theo danh mục") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Lọc theo cấu hình xe") @RequestParam(required = false) Long vehicleConfigurationId,
            @Parameter(description = "Lọc theo node xe") @RequestParam(required = false) Long vehicleNodeId,
            @Parameter(description = "Lọc theo hãng xe") @RequestParam(required = false) Long brandId,
            @Parameter(description = "Lọc theo dòng xe") @RequestParam(required = false) Long modelId,
            @Parameter(description = "Lọc theo người dùng tạo (chỉ user thuộc đại lý)") @RequestParam(required = false) Long userId,
            @Parameter(description = "Lọc từ ngày tạo (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @Parameter(description = "Lọc đến ngày tạo (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @Parameter(description = "Trạng thái") @RequestParam(required = false) String status,
            @Parameter(description = "Số trang (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sắp xếp") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getDealer() == null) {
            return ResponseEntity.ok(PageResponse.<UserSavedFileResponse>builder()
                    .content(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(0L)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build());
        }

        Long dealerId = currentUser.getDealer().getId();
        Long effectiveUserId = userId;
        if (userId != null) {
            boolean belongs = userRepository.findById(userId).filter(u -> !u.isDeleted())
                    .map(u -> u.getDealer() != null && u.getDealer().getId().equals(dealerId))
                    .orElse(false);
            if (!belongs) {
                return ResponseEntity.ok(PageResponse.<UserSavedFileResponse>builder()
                        .content(Collections.emptyList())
                        .page(page)
                        .size(size)
                        .totalElements(0L)
                        .totalPages(0)
                        .first(true)
                        .last(true)
                        .build());
            }
        }

        Long effectiveNodeId = vehicleNodeId != null ? vehicleNodeId : vehicleConfigurationId;
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortObj);

        PageResponse<UserSavedFileResponse> response = userSvgFileService.listAdminFiles(
                keyword, categoryId, effectiveNodeId, brandId, modelId, dealerId, effectiveUserId,
                createdFrom, createdTo, status, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết metadata bản SVG đã lưu",
            description = "Trả về thông tin chi tiết metadata của bản lưu thuộc đại lý của AGENT (chỉ đọc, không cho xem storage path hoặc plaintext).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thông tin chi tiết bản lưu"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập file của đại lý khác", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bản lưu", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserSavedFileResponse> getFileDetail(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        UserSavedFileResponse detail = userSvgFileService.getAdminFile(id);
        if (detail.getDealer() == null || currentUser.getDealer() == null ||
                !detail.getDealer().getId().equals(currentUser.getDealer().getId())) {
            throw new ForbiddenException("Không có quyền truy cập file của đại lý khác");
        }
        return ResponseEntity.ok(detail);
    }

    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        String prop = parts[0].trim();
        Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(dir, prop);
    }
}

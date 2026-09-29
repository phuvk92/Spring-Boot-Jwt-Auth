package com.example.svgmanager.controller;

import com.example.svgmanager.dto.request.CreateVehicleNodeRequest;
import com.example.svgmanager.dto.request.UpdateVehicleNodeRequest;
import com.example.svgmanager.dto.response.DeleteVehicleNodeResponse;
import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.VehicleNodeImpactResponse;
import com.example.svgmanager.dto.response.VehicleNodeResponse;
import com.example.svgmanager.service.VehicleNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Quản trị cây xe 4 cấp BRAND › SERIES › MODEL › SUBTYPE (SA-DanhMucXe-v2 §3.1).
 */
@RestController
@RequestMapping("/api/vehicle-nodes")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Vehicle Nodes", description = "Cây danh mục xe 4 cấp — Data Center v2")
public class VehicleNodeController {

    private final VehicleNodeService vehicleNodeService;

    public VehicleNodeController(VehicleNodeService vehicleNodeService) {
        this.vehicleNodeService = vehicleNodeService;
    }

    @GetMapping
    @Operation(summary = "Danh sách hãng kèm cả cây con, phân trang theo hãng",
            description = "Có q → chỉ giữ node khớp tên và mọi tổ tiên của nó.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trang hãng kèm cây con"),
            @ApiResponse(responseCode = "403", description = "Không phải ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<VehicleNodeResponse>> getTree(
            @Parameter(description = "Tìm theo tên node ở mọi cấp") @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(vehicleNodeService.getBrandTrees(q, page, size));
    }

    @PostMapping
    @Operation(summary = "Tạo node mới", description = "parentId null → hãng (BRAND); cấp con suy từ cha.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Node đã tạo"),
            @ApiResponse(responseCode = "400", description = "Cha là SUBTYPE hoặc dữ liệu sai",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không có node cha",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Trùng tên trong cùng cha (NODE_NAME_TAKEN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehicleNodeResponse> create(@Valid @RequestBody CreateVehicleNodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleNodeService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Đổi tên node", description = "Không đổi cha/cấp.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã đổi tên"),
            @ApiResponse(responseCode = "404", description = "Không có node",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Trùng tên trong cùng cha (NODE_NAME_TAKEN)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehicleNodeResponse> rename(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleNodeRequest request
    ) {
        return ResponseEntity.ok(vehicleNodeService.rename(id, request));
    }

    @GetMapping("/{id}/impact")
    @Operation(summary = "Số liệu trước khi xoá", description = "{nodes, files} — số node con sẽ mất và số file mất liên kết.")
    public ResponseEntity<VehicleNodeImpactResponse> impact(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleNodeService.impact(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá node và cả nhánh con", description = "Không chặn khi còn file (Q4) — file chỉ mất liên kết.")
    public ResponseEntity<DeleteVehicleNodeResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleNodeService.delete(id));
    }
}

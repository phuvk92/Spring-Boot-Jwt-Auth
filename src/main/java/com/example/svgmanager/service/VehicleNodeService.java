package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.CreateVehicleNodeRequest;
import com.example.svgmanager.dto.request.UpdateVehicleNodeRequest;
import com.example.svgmanager.dto.response.DeleteVehicleNodeResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.VehicleNodeImpactResponse;
import com.example.svgmanager.dto.response.VehicleNodeResponse;

/**
 * Quản trị cây xe 4 cấp (SA-DanhMucXe-v2 §3.1) — chỉ ADMIN gọi.
 */
public interface VehicleNodeService {

    /**
     * Phân trang THEO HÃNG; mỗi hãng kèm cả cây con. Có {@code q} thì chỉ giữ node
     * khớp tên và mọi tổ tiên của nó (hãng vẫn là gốc của trang).
     */
    PageResponse<VehicleNodeResponse> getBrandTrees(String q, int page, int size);

    /** Tạo node; parentId null → BRAND. Cấp suy từ cha; SUBTYPE không có con. */
    VehicleNodeResponse create(CreateVehicleNodeRequest request);

    /** Chỉ đổi tên — không đổi cha/cấp. Trùng tên cùng cha → 409 NODE_NAME_TAKEN. */
    VehicleNodeResponse rename(Long id, UpdateVehicleNodeRequest request);

    /** Số liệu cho hộp xác nhận xoá: số node con + số file sẽ mất liên kết. */
    VehicleNodeImpactResponse impact(Long id);

    /** Xoá cả nhánh — KHÔNG chặn (Q4); file chỉ mất liên kết. */
    DeleteVehicleNodeResponse delete(Long id);
}

package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.DesignFileDto;
import com.example.svgmanager.dto.response.DesignFileGeometryDto;
import com.example.svgmanager.dto.response.PartDto;

import java.util.List;

/**
 * Kho file thiết kế cho app cắt — hợp đồng openapi v0.3.0 (KX-30 · KX-32 · KX-35).
 */
public interface DesignFileService {

    /**
     * File khớp bộ lọc của app thợ (SA-DanhMucXe-v2 §3.3, hợp đồng v0.6):
     * categoryId và modelId bắt buộc (thiếu → BadRequestException).
     * Có subtypeId → khớp subtype hoặc model cha; không → khớp model hoặc mọi phiên bản.
     * year null → mọi năm; có year thì file không ghi năm (NULL) vẫn khớp (Q3).
     */
    List<DesignFileDto> getFiles(String categoryId, String modelId, String subtypeId, Integer year);

    /**
     * Part bên trong một file, theo thứ tự đội nội dung dựng.
     * File không tồn tại → 404 FILE_NOT_FOUND, KHÔNG trả mảng rỗng.
     */
    List<PartDto> getFileParts(String fileKey);

    /**
     * Hình học hiển thị của cả file — MỘT lượt tải cho MỘT tab Design Center
     * (F-56 · KX-43 · DS-08c). Lệnh cắt không sinh từ dữ liệu này (RB-07).
     * File không tồn tại → 404 FILE_NOT_FOUND.
     */
    DesignFileGeometryDto getFileGeometry(String fileKey);
}

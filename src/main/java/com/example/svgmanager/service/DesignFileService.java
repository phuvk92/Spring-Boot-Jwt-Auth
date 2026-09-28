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
     * File thiết kế của chiếc xe đã lọc đủ 6 cấp. Rỗng = xe có trong danh mục (hoặc đường
     * danh mục không khớp) nhưng chưa nạp file — KX-35: không bao giờ null, không 404.
     */
    List<DesignFileDto> getFiles(String category, String brand, String model,
                                 String variant, String year, String submodel);

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

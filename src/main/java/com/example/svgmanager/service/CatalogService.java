package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.CatalogOptionDto;

import java.util.List;

/**
 * Danh mục cho app thợ — Data Center v2 (SA-DanhMucXe-v2 §3.3): cây xe 4 cấp
 * BRAND › SERIES › MODEL › SUBTYPE lọc theo id cha, danh mục file từ file_categories,
 * cấp "year" trả các năm có file khớp bộ lọc.
 */
public interface CatalogService {

    /** Danh mục file đang hiệu lực (Ngoại thất · Nội thất · Window film · Đèn & kính…). */
    List<CatalogOptionDto> getFileCategories();

    /**
     * Giá trị hợp lệ của một cấp. Tham số id theo cấp:
     * {@code series} cần brandId · {@code model} cần seriesId · {@code subtype} cần modelId —
     * thiếu → BadRequestException (400), không tự điền giá trị mẫu.
     * {@code year} không nằm trong cây: lọc theo categoryId/modelId/subtypeId đã chọn,
     * tham số nào truyền thì lọc theo tham số đó.
     */
    List<CatalogOptionDto> getCatalog(String level, String brandId, String seriesId,
                                      String modelId, String subtypeId, String categoryId);
}

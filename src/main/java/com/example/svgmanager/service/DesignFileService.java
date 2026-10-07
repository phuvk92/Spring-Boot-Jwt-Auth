package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.DesignFileDto;

import com.example.svgmanager.dto.response.PageResponse;

import java.util.List;

/**
 * Kho file thiết kế cho app cắt — hợp đồng openapi v0.6 (KX-30 · KX-32 · KX-35).
 */
public interface DesignFileService {

    /**
     * File khớp bộ lọc của app thợ (SA-DanhMucXe-v2 §3.3, hợp đồng v0.6):
     * Mọi tham số tuỳ chọn, phân trang, sắp xếp updatedAt giảm dần.
     * Cấp xe: brandId / seriesId / modelId khớp file gắn node hoặc node con.
     * subtypeId khớp file gắn subtype hoặc model cha.
     * year: model_year = year HOẶC NULL (Q3).
     * Chỉ trả file ACTIVE, không lọc theo đại lý (Q6).
     */
    PageResponse<DesignFileDto> getFiles(String q, Long categoryId, Integer year,
                                         Long brandId, Long seriesId, Long modelId, Long subtypeId,
                                         int page, int size);

    /**
     * Nội dung SVG của một bản (đã khử độc lúc upload) — app tự tách part (board 08/10,
     * SA-Nesting §8). {@code layout} = "nested" | "raw"; bỏ trống → bản đã xếp nếu có.
     * File không tồn tại hoặc không có bản được hỏi → 404 FILE_NOT_FOUND.
     */
    String getFileSvg(String fileKey, String layout);
}

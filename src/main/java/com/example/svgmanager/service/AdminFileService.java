package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.AdminFileResponse;
import com.example.svgmanager.dto.response.AdminFileStatsResponse;
import com.example.svgmanager.dto.response.PageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Kho part file — SA-DanhMucXe-v2 §3.2. Chỉ ADMIN (web quản trị).
 */
public interface AdminFileService {

    PageResponse<AdminFileResponse> getFiles(String q, Long categoryId, Integer year,
                                             Long brandId, Long seriesId, Long modelId,
                                             int page, int size);

    AdminFileStatsResponse getStats();

    /** Upload file mới: tách part + hình học theo SA §4, sinh file_key, gắn mẫu xe. */
    AdminFileResponse createFile(MultipartFile file, String name, Long categoryId, Integer year,
                                 List<Long> vehicleNodeIds, MultipartFile thumbnail);

    /**
     * Sửa file. {@code file} tuỳ chọn — có file mới thì tách lại và thay toàn bộ
     * svg_file_parts. {@code yearPresent} báo client có gửi tham số year (kể cả rỗng =
     * xoá năm → hiện mọi năm); cách tham số còn lại: null = giữ nguyên.
     */
    AdminFileResponse updateFile(Long id, MultipartFile file, String name, Long categoryId,
                                 Integer year, boolean yearPresent,
                                 List<Long> vehicleNodeIds, MultipartFile thumbnail);

    /** Xoá mềm — status = DELETED, bản ghi và file vật lý còn để audit. */
    void deleteFile(Long id);
}

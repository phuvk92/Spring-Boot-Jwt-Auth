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

    /** Upload file mới: nestedFile? + rawFile? (>= 1) — tách part + hình học theo SA §4/§8, sinh file_key, gắn mẫu xe. */
    AdminFileResponse createFile(MultipartFile nestedFile, MultipartFile rawFile,
                                 String name, Long categoryId, Integer year,
                                 List<Long> vehicleNodeIds, MultipartFile thumbnail,
                                 Integer cutAreaLengthMm, Integer cutAreaWidthMm);

    /**
     * Sửa file: thay riêng từng file hoặc xoá một bản (removeNested/removeRaw). Không được bỏ cả hai.
     */
    AdminFileResponse updateFile(Long id, MultipartFile nestedFile, MultipartFile rawFile,
                                 boolean removeNested, boolean removeRaw,
                                 String name, Long categoryId,
                                 Integer year, boolean yearPresent,
                                 List<Long> vehicleNodeIds, MultipartFile thumbnail,
                                 Integer cutAreaLengthMm, Integer cutAreaWidthMm, boolean clearCutArea);

    /** Xoá mềm — status = DELETED, bản ghi và file vật lý còn để audit. */
    void deleteFile(Long id);
}

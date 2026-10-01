package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.SvgResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.MediaType;

public interface SvgService {

    SvgResponse uploadSvg(MultipartFile file);

    PageResponse<SvgResponse> getSvgFiles(
            String keyword,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    SvgResponse getSvgFileById(Long id);

    SvgResponse updateSvg(Long id, String status);

    default Resource previewSvg(Long id) {
        return previewSvg(id, null);
    }

    Resource previewSvg(Long id, String layout);

    default Resource downloadSvg(Long id) {
        return downloadSvg(id, null);
    }

    Resource downloadSvg(Long id, String layout);

    default String getOriginalFilename(Long id) {
        return getOriginalFilename(id, null);
    }

    String getOriginalFilename(Long id, String layout);

    /** Ảnh xem trước tuỳ chọn do đội nội dung gắn (SA v2 §3.2 — thumbnail_path). */
    Resource thumbnailSvg(Long id);

    MediaType thumbnailContentType(Long id);

    void deleteSvg(Long id);
}

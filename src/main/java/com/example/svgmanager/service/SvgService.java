package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.SvgResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface SvgService {

    SvgResponse uploadSvg(MultipartFile file, Long categoryId);

    PageResponse<SvgResponse> getSvgFiles(
            String keyword,
            Long categoryId,
            Long uploadedBy,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    SvgResponse getSvgFileById(Long id);

    SvgResponse updateSvg(Long id, Long categoryId);

    Resource previewSvg(Long id);

    Resource downloadSvg(Long id);

    String getOriginalFilename(Long id);

    void deleteSvg(Long id);
}

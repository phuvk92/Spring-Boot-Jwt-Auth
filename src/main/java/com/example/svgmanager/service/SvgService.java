package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.SvgResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface SvgService {

    SvgResponse uploadSvg(MultipartFile file);

    PageResponse<SvgResponse> getSvgFiles(
            String keyword,
            Long uploadedBy,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    SvgResponse getSvgFileById(Long id);

    Resource previewSvg(Long id);

    Resource downloadSvg(Long id);

    String getOriginalFilename(Long id);

    void deleteSvg(Long id);
}

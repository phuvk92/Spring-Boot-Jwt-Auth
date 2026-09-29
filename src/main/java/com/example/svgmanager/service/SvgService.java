package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.BatchSvgUploadResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.SvgResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SvgService {

    SvgResponse uploadSvg(MultipartFile file);

    BatchSvgUploadResponse batchUploadSvg(List<MultipartFile> files);

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

    Resource previewSvg(Long id);

    Resource downloadSvg(Long id);

    String getOriginalFilename(Long id);

    void deleteSvg(Long id);
}

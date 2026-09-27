package com.example.svgmanager.service;

import com.example.svgmanager.dto.internal.InternalSvgDetailResponse;
import org.springframework.core.io.Resource;

public interface InternalSvgService {

    default InternalSvgDetailResponse getSvgDetail(Long svgFileId) {
        return getSvgDetail(svgFileId, null, null);
    }

    InternalSvgDetailResponse getSvgDetail(Long svgFileId, String device, String ipAddress);

    default Resource downloadSvg(Long svgFileId) {
        return downloadSvg(svgFileId, null, null);
    }

    Resource downloadSvg(Long svgFileId, String device, String ipAddress);

    String getOriginalFilename(Long svgFileId);
}

package com.example.svgmanager.dto.response;

import java.util.List;

public record BatchSvgUploadResponse(
        List<SvgResponse> files,
        int totalUploaded
) {
}

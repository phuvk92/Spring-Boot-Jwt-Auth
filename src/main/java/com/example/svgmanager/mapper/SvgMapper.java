package com.example.svgmanager.mapper;

import com.example.svgmanager.dto.response.SvgResponse;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.service.CategoryService;
import org.springframework.stereotype.Component;

@Component
public class SvgMapper {

    private final UserMapper userMapper;
    private final CategoryService categoryService;

    public SvgMapper(UserMapper userMapper, CategoryService categoryService) {
        this.userMapper = userMapper;
        this.categoryService = categoryService;
    }

    public SvgResponse toSvgResponse(SvgFile svgFile) {
        if (svgFile == null) {
            return null;
        }

        Long agentId = svgFile.getAgent() != null ? svgFile.getAgent().getId() : null;

        return SvgResponse.builder()
                .id(svgFile.getId())
                .originalFilename(svgFile.getOriginalFilename())
                .storedFilename(svgFile.getStoredFilename())
                .fileSize(svgFile.getFileSize())
                .contentType(svgFile.getContentType())
                .checksum(svgFile.getChecksum())
                .category(svgFile.getCategory() != null ? categoryService.toSummary(svgFile.getCategory()) : null)
                .uploadedBy(userMapper.toUserSummaryResponse(svgFile.getUploadedBy()))
                .agentId(agentId)
                .createdAt(svgFile.getCreatedAt())
                .updatedAt(svgFile.getUpdatedAt())
                .build();
    }
}

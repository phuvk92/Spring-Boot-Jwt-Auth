package com.example.svgmanager.mapper;

import com.example.svgmanager.dto.response.SvgResponse;
import com.example.svgmanager.entity.SvgFile;
import org.springframework.stereotype.Component;

@Component
public class SvgMapper {

    private final UserMapper userMapper;

    public SvgMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
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
                .uploadedBy(userMapper.toUserSummaryResponse(svgFile.getUploadedBy()))
                .agentId(agentId)
                .createdAt(svgFile.getCreatedAt())
                .updatedAt(svgFile.getUpdatedAt())
                .build();
    }
}

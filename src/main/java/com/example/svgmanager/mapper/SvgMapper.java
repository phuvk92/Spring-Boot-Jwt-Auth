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
        return SvgResponse.builder()
                .id(svgFile.getId())
                .originalFilename(svgFile.getOriginalFilename())
                .fileSize(svgFile.getFileSize())
                .contentType(svgFile.getContentType())
                .checksum(svgFile.getChecksum())
                .uploadedBy(userMapper.toUserSummaryResponse(svgFile.getUploadedBy()))
                .createdAt(svgFile.getCreatedAt())
                .updatedAt(svgFile.getUpdatedAt())
                .build();
    }
}

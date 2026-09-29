package com.example.svgmanager.mapper;

import com.example.svgmanager.dto.response.SvgResponse;
import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.User;
import org.springframework.stereotype.Component;

@Component
public class SvgMapper {

    private final UserMapper userMapper;

    public SvgMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public SvgResponse toSvgResponse(SvgFile svgFile) {
        return toSvgResponse(svgFile, null, true);
    }

    public SvgResponse toSvgResponse(SvgFile svgFile, User currentUser, boolean isAdmin) {
        if (svgFile == null) {
            return null;
        }

        Long agentId = svgFile.getAgent() != null ? svgFile.getAgent().getId() : null;

        // Q6 (chốt 29/09): không còn phân quyền đại lý — phiên hợp lệ là xem/tải được
        boolean canView = true;
        boolean canDownload = true;

        return SvgResponse.builder()
                .id(svgFile.getId())
                .originalFilename(svgFile.getOriginalFilename())
                .storedFilename(svgFile.getStoredFilename())
                .fileSize(svgFile.getFileSize())
                .contentType(svgFile.getContentType())
                .checksum(svgFile.getChecksum())
                .status(svgFile.getStatus())
                .fileCategory(svgFile.getFileCategory() != null ? svgFile.getFileCategory().getName() : null)
                .modelYear(svgFile.getModelYear())
                .source(svgFile.getSource())
                .canView(canView)
                .canDownload(canDownload)
                .uploadedBy(userMapper.toUserSummaryResponse(svgFile.getUploadedBy()))
                .agentId(agentId)
                .createdAt(svgFile.getCreatedAt())
                .updatedAt(svgFile.getUpdatedAt())
                .build();
    }
}

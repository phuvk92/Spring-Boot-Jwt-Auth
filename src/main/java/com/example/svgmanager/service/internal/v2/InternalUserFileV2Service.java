package com.example.svgmanager.service.internal.v2;

import com.example.svgmanager.dto.internal.v2.FileSharesV2Response;
import com.example.svgmanager.dto.internal.v2.UserSvgFileV2Response;
import com.example.svgmanager.dto.internal.v2.UserSvgFileV2ShareResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;

public interface InternalUserFileV2Service {

    default UserSvgFileV2Response saveUserFile(
            User currentUser,
            byte[] fileBytes,
            String originalFilename,
            String customFileName,
            Long categoryId,
            Long vehicleNodeId,
            String brandName,
            String modelName,
            Integer yearFrom,
            Integer yearTo,
            String generationCode,
            String productGroup,
            String productGroupName,
            Double filmWidth,
            String filmWidthUnit,
            Double rollLength,
            String rollLengthUnit,
            Double axisX,
            Double axisY,
            String sourceFileKey,
            String description
    ) {
        return saveUserFile(
                currentUser,
                fileBytes,
                originalFilename,
                "image/svg+xml",
                customFileName,
                categoryId,
                vehicleNodeId,
                brandName,
                modelName,
                yearFrom,
                yearTo,
                generationCode,
                productGroup,
                productGroupName,
                filmWidth,
                filmWidthUnit,
                rollLength,
                rollLengthUnit,
                axisX,
                axisY,
                sourceFileKey,
                description
        );
    }

    UserSvgFileV2Response saveUserFile(
            User currentUser,
            byte[] fileBytes,
            String originalFilename,
            String mimeType,
            String customFileName,
            Long categoryId,
            Long vehicleNodeId,
            String brandName,
            String modelName,
            Integer yearFrom,
            Integer yearTo,
            String generationCode,
            String productGroup,
            String productGroupName,
            Double filmWidth,
            String filmWidthUnit,
            Double rollLength,
            String rollLengthUnit,
            Double axisX,
            Double axisY,
            String sourceFileKey,
            String description
    );

    default UserSvgFileV2Response updateUserFile(
            User currentUser,
            Long id,
            byte[] fileBytes,
            String originalFilename,
            String customFileName,
            Long categoryId,
            Long vehicleNodeId,
            String brandName,
            String modelName,
            Integer yearFrom,
            Integer yearTo,
            String generationCode,
            String productGroup,
            String productGroupName,
            Double filmWidth,
            String filmWidthUnit,
            Double rollLength,
            String rollLengthUnit,
            Double axisX,
            Double axisY,
            String sourceFileKey,
            String description
    ) {
        return updateUserFile(
                currentUser,
                id,
                fileBytes,
                originalFilename,
                "image/svg+xml",
                customFileName,
                categoryId,
                vehicleNodeId,
                brandName,
                modelName,
                yearFrom,
                yearTo,
                generationCode,
                productGroup,
                productGroupName,
                filmWidth,
                filmWidthUnit,
                rollLength,
                rollLengthUnit,
                axisX,
                axisY,
                sourceFileKey,
                description
        );
    }

    UserSvgFileV2Response updateUserFile(
            User currentUser,
            Long id,
            byte[] fileBytes,
            String originalFilename,
            String mimeType,
            String customFileName,
            Long categoryId,
            Long vehicleNodeId,
            String brandName,
            String modelName,
            Integer yearFrom,
            Integer yearTo,
            String generationCode,
            String productGroup,
            String productGroupName,
            Double filmWidth,
            String filmWidthUnit,
            Double rollLength,
            String rollLengthUnit,
            Double axisX,
            Double axisY,
            String sourceFileKey,
            String description
    );

    PageResponse<UserSvgFileV2Response> listUserFiles(
            User currentUser,
            String keyword,
            Long categoryId,
            String status,
            Pageable pageable
    );

    UserSvgFileV2Response getUserFile(User currentUser, Long id);

    Resource downloadUserFile(User currentUser, Long id);

    byte[] getUserFileBytes(User currentUser, Long id);

    void deleteUserFile(User currentUser, Long id);

    UserSvgFileV2ShareResponse shareFile(User currentUser, Long fileId, Long targetUserId);

    FileSharesV2Response getShares(User currentUser, Long fileId);

    void revokeShare(User currentUser, Long fileId, Long targetUserId);

    String getOriginalFilename(Long id);
}

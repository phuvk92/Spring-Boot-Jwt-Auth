package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.dto.response.UserSavedFileResponse;
import com.example.svgmanager.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface UserSvgFileService {

    UserSavedFileResponse saveUserFile(
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
    );

    UserSavedFileResponse updateUserFile(
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
    );

    PageResponse<UserSavedFileResponse> listAdminFiles(
            String keyword,
            Long categoryId,
            Long vehicleNodeId,
            Long brandId,
            Long modelId,
            Long dealerId,
            Long userId,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            String status,
            Pageable pageable
    );

    PageResponse<UserSavedFileResponse> listUserFiles(
            User currentUser,
            String keyword,
            Long categoryId,
            String status,
            Pageable pageable
    );

    UserSavedFileResponse getAdminFile(Long id);

    UserSavedFileResponse getUserFile(User currentUser, Long id);

    Resource downloadAdminFile(Long id);

    Resource downloadUserFile(User currentUser, Long id);

    String getOriginalFilename(Long id);

    byte[] getAdminFileBytes(Long id);
}

package com.example.svgmanager.service;

import com.example.svgmanager.dto.request.AdminRevokeShareRequest;
import com.example.svgmanager.dto.response.AdminShareActivityResponse;
import com.example.svgmanager.dto.response.AdminShareItemResponse;
import com.example.svgmanager.dto.response.AdminShareStatsResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminShareService {

    PageResponse<AdminShareItemResponse> listShares(
            String shareType,
            String status,
            String keyword,
            Long dealerId,
            Long fileId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    AdminShareItemResponse getShareDetail(String shareId);

    List<AdminShareActivityResponse> getShareHistory(String shareId);

    PageResponse<AdminShareActivityResponse> listActivityHistory(
            String shareId,
            Long fileId,
            String action,
            String result,
            String keyword,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    AdminShareStatsResponse getStatistics();

    AdminShareItemResponse revokeShare(
            User currentUser,
            String shareId,
            AdminRevokeShareRequest request,
            HttpServletRequest httpRequest
    );
}

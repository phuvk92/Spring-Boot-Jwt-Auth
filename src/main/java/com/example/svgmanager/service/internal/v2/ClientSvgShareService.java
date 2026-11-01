package com.example.svgmanager.service.internal.v2;

import com.example.svgmanager.dto.internal.v2.ClientShareResponse;
import com.example.svgmanager.dto.internal.v2.CreateClientShareRequest;
import com.example.svgmanager.dto.internal.v2.VerifyClientShareResponse;
import com.example.svgmanager.dto.response.PageResponse;
import com.example.svgmanager.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ClientSvgShareService {

    ClientShareResponse createShare(User currentUser, CreateClientShareRequest request, HttpServletRequest httpRequest);

    PageResponse<ClientShareResponse> listShares(
            User currentUser,
            String status,
            Long fileId,
            String keyword,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable,
            HttpServletRequest httpRequest
    );

    ClientShareResponse getShare(User currentUser, String shareId, HttpServletRequest httpRequest);

    ClientShareResponse revokeShare(User currentUser, String shareId, String reason, HttpServletRequest httpRequest);

    VerifyClientShareResponse verifyShare(String shareId, String password, HttpServletRequest httpRequest);

    byte[] previewShare(String shareId, String shareToken, HttpServletRequest httpRequest);

    byte[] downloadShare(String shareId, String shareToken, HttpServletRequest httpRequest);

    String getShareFilename(String shareId);
}

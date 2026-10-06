package com.example.svgmanager.service;

import com.example.svgmanager.dto.response.FileSharesResponse;
import com.example.svgmanager.dto.response.UserSvgFileShareResponse;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserSvgFile;

public interface UserSvgFileShareService {

    /**
     * Chia sẻ file SVG cho một User khác.
     * Chỉ Owner của file hoặc ADMIN mới có quyền. Không cho phép AGENT hoặc User không sở hữu.
     */
    UserSvgFileShareResponse shareFile(User currentUser, Long fileId, Long targetUserId);

    /**
     * Lấy danh sách user đang được chia sẻ file SVG.
     * Chỉ Owner của file hoặc ADMIN mới có quyền.
     */
    FileSharesResponse getShares(User currentUser, Long fileId);

    /**
     * Thu hồi quyền chia sẻ file SVG từ một User.
     * Chỉ Owner của file hoặc ADMIN mới có quyền.
     */
    void revokeShare(User currentUser, Long fileId, Long targetUserId);

    /**
     * Kiểm tra quyền truy cập file (xem, preview, download):
     * True nếu là ADMIN, hoặc là Owner, hoặc có share ACTIVE.
     */
    boolean canAccess(UserSvgFile file, User currentUser);

    /**
     * Kiểm tra quyền quản lý chia sẻ file:
     * True nếu là ADMIN, hoặc là Owner (người tạo file).
     */
    boolean canManageShares(UserSvgFile file, User currentUser);
}

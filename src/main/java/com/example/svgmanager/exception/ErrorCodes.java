package com.example.svgmanager.exception;

/**
 * Mã lỗi trong trường {@code code} của ErrorResponse.
 * Tên trùng với hợp đồng của Pcut-Client ({@code contracts/openapi.yaml}) — đổi tên là client hiểu sai.
 */
public final class ErrorCodes {

    /** Tài khoản đã đủ số máy đăng ký (F-57). */
    public static final String SESSION_LIMIT = "SESSION_LIMIT";
    /** Máy đã bị gỡ khỏi tài khoản, hoặc phiên không còn gắn với máy nào đang hoạt động. */
    public static final String SESSION_REVOKED = "SESSION_REVOKED";
    /** Request của app cắt thiếu định danh thiết bị. */
    public static final String DEVICE_REQUIRED = "DEVICE_REQUIRED";
    /** Tài khoản thợ (USER) đăng nhập trang quản trị web. */
    public static final String USER_WEB_LOGIN_FORBIDDEN = "USER_WEB_LOGIN_FORBIDDEN";
    /** Keycloak không phản hồi. */
    public static final String AUTH_SERVICE_UNAVAILABLE = "AUTH_SERVICE_UNAVAILABLE";
    /** GET /api/v1/files/{id}/parts với id không tồn tại — khác với "file không có part nào" (mảng rỗng). */
    public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";

    private ErrorCodes() {
    }
}

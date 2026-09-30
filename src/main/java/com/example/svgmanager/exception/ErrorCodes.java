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
    /** Tạo/đổi tên node xe trùng tên node khác trong cùng cha (V14). */
    public static final String NODE_NAME_TAKEN = "NODE_NAME_TAKEN";
    /** Upload kho part file với định dạng không phải SVG (SA-DanhMucXe-v2 Q7). */
    public static final String UNSUPPORTED_FORMAT = "UNSUPPORTED_FORMAT";
    /** SVG không khai đơn vị (width/height + viewBox) — server không hỏi lại được thợ (DS-108). */
    public static final String SVG_UNITS_MISSING = "SVG_UNITS_MISSING";
    /** GET /api/v1/designs/{id}/versions với id không tồn tại hoặc không phải bản của user — khác với mảng rỗng. */
    public static final String DESIGN_NOT_FOUND = "DESIGN_NOT_FOUND";

    private ErrorCodes() {
    }
}

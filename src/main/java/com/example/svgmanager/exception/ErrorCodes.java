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
    /** Tài khoản người dùng đã hết hạn sử dụng. */
    public static final String USER_ACCOUNT_EXPIRED = "USER_ACCOUNT_EXPIRED";
    /** GET /api/v1/files/{id}/parts với id không tồn tại — khác với "file không có part nào" (mảng rỗng). */
    public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
    /** Tạo/đổi tên node xe trùng tên node khác trong cùng cha (V15). */
    public static final String NODE_NAME_TAKEN = "NODE_NAME_TAKEN";
    /** Upload/sửa part file gửi nhiều hơn một mẫu xe — board 30/09: một file một mẫu xe. */
    public static final String ONE_VEHICLE_PER_FILE = "ONE_VEHICLE_PER_FILE";
    /** Upload kho part file với định dạng không phải SVG (SA-DanhMucXe-v2 Q7). */
    public static final String UNSUPPORTED_FORMAT = "UNSUPPORTED_FORMAT";
    /** SVG không khai đơn vị (width/height + viewBox) — server không hỏi lại được thợ (DS-108). */
    public static final String SVG_UNITS_MISSING = "SVG_UNITS_MISSING";
    /** GET /api/v1/designs/{id}/versions với id không tồn tại hoặc không phải bản của user — khác với mảng rỗng. */
    public static final String DESIGN_NOT_FOUND = "DESIGN_NOT_FOUND";
    /** Upload/sửa part file thiếu cả hai bản nested và raw (SA-DanhMucXe-v2 §8). */
    public static final String FILE_REQUIRED = "FILE_REQUIRED";
    /** Khai khổ cắt chỉ một trong hai trường (epic NGO-399). */
    public static final String CUT_AREA_INCOMPLETE = "CUT_AREA_INCOMPLETE";
    /** Khổ cắt ngoài giới hạn: chiều dài 100–50000 mm, khổ phim 100–2000 mm (epic NGO-399). */
    public static final String CUT_AREA_OUT_OF_RANGE = "CUT_AREA_OUT_OF_RANGE";
    /** Bản làm việc vượt quá dung lượng tối đa cho phép (20 MB) — F-36. */
    public static final String DESIGN_TOO_LARGE = "DESIGN_TOO_LARGE";
    /** Payload bản làm việc rỗng hoặc không hợp lệ — F-36. */
    public static final String DESIGN_INVALID = "DESIGN_INVALID";

    /** Không tìm thấy bản lưu file SVG của người dùng. */
    public static final String USER_FILE_NOT_FOUND = "USER_FILE_NOT_FOUND";
    /** Không tìm thấy quyền chia sẻ file SVG. */
    public static final String USER_FILE_SHARE_NOT_FOUND = "USER_FILE_SHARE_NOT_FOUND";
    /** Quyền chia sẻ file SVG cho người dùng đã tồn tại. */
    public static final String USER_FILE_SHARE_ALREADY_EXISTS = "USER_FILE_SHARE_ALREADY_EXISTS";
    /** Không có quyền thực hiện thao tác chia sẻ hoặc truy cập file chia sẻ. */
    public static final String USER_FILE_SHARE_FORBIDDEN = "USER_FILE_SHARE_FORBIDDEN";
    /** Người dùng nhận quyền chia sẻ không hợp lệ hoặc không tồn tại. */
    public static final String INVALID_SHARE_TARGET = "INVALID_SHARE_TARGET";
    /** Không thể chia sẻ file cho chính mình. */
    public static final String CANNOT_SHARE_TO_SELF = "CANNOT_SHARE_TO_SELF";

    /** File SVG vi phạm chính sách bảo mật (XSS, XXE, SSRF, Active Content, etc.). */
    public static final String SVG_SECURITY_VALIDATION_FAILED = "SVG_SECURITY_VALIDATION_FAILED";
    /** File SVG vượt quá giới hạn kích thước, độ sâu XML hoặc số phần tử cho phép. */
    public static final String SVG_SIZE_LIMIT_EXCEEDED = "SVG_SIZE_LIMIT_EXCEEDED";
    /** Phát hiện chữ ký malware hoặc cấu trúc nén độc hại trong file. */
    public static final String SVG_MALWARE_DETECTED = "SVG_MALWARE_DETECTED";
    /** Vượt quá tần suất gọi API cho phép (Rate Limit). */
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";

    private ErrorCodes() {
    }
}

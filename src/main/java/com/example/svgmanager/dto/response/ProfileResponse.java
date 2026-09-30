package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Hồ sơ đầy đủ cho màn Thông tin cá nhân của app cắt (F-07 / KX-04) — {@code GET /api/v1/profile}.
 *
 * Quy ước null ≠ rỗng (KX-54): trường chưa có dữ liệu trả {@code null}/{@code []} tường minh,
 * không ném lỗi — client rơi về dữ liệu phiên đang giữ.
 *
 * ⚠ {@code plan} hiện lấy nhãn gói trên hồ sơ đại lý ({@code dealers.plan}) — mô hình gói thật
 * chờ C1/C10/C11 chốt với khách. {@code quota} luôn {@code null}: hạn mức m² chờ A6a, client giữ
 * nguyên chuỗi server trả nên không tự bịa.
 */
@Schema(description = "Hồ sơ đầy đủ của người dùng hiện tại (F-07 / KX-04)")
public record ProfileResponse(
        UserBlock user,
        @Schema(description = "Đại lý trực thuộc — null khi tài khoản không thuộc đại lý") DealerBlock dealer,
        @Schema(description = "Gói cước — null khi chưa cấu hình gói (không có đại lý / đại lý chưa gán gói)") PlanBlock plan,
        @Schema(description = "Hạn mức m² dạng chuỗi nguyên văn — null khi chưa có dữ liệu (chờ A6a)") QuotaBlock quota,
        @Schema(description = "Máy đã đăng ký của tài khoản — cùng khuôn /api/internal/devices (NGO-273), gồm cả máy REVOKED")
        List<UserDeviceResponse> devices
) {
    @Schema(description = "Khối người dùng: tên, vai trò, liên hệ nếu có")
    public record UserBlock(
            Long id,
            String username,
            @Schema(description = "Tên hiển thị — rơi về username khi chưa nhập họ tên") String displayName,
            String role,
            String email,
            @Schema(description = "Số điện thoại — null khi chưa khai báo") String phone) {
    }

    @Schema(description = "Khối đại lý: tên + mã")
    public record DealerBlock(Long id, String code, String name) {
    }

    @Schema(description = "Khối gói cước — expiresAt/seats null khi chưa có mô hình gói thật")
    public record PlanBlock(
            String name,
            @Schema(description = "Thời điểm hết hạn — null: due_date của đại lý là chuỗi tự do, chưa đổi được") LocalDateTime expiresAt,
            Integer seatsUsed,
            Integer seatsTotal) {
    }

    @Schema(description = "Khối hạn mức — ba chuỗi nguyên văn server tính, client không tự tính")
    public record QuotaBlock(String remaining, String used, String period) {
    }
}

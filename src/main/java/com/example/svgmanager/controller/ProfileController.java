package com.example.svgmanager.controller;

import com.example.svgmanager.dto.response.ErrorResponse;
import com.example.svgmanager.dto.response.ProfileResponse;
import com.example.svgmanager.dto.response.UserDeviceResponse;
import com.example.svgmanager.entity.Dealer;
import com.example.svgmanager.entity.User;
import com.example.svgmanager.security.CurrentUserService;
import com.example.svgmanager.service.UserDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * F-07 / KX-04 — hồ sơ cá nhân của app cắt. Định danh theo {@code sub} của token, client
 * không gửi userId nên không có đường xem hồ sơ người khác.
 */
@RestController
@RequestMapping("/api/v1/profile")
@PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Profile", description = "Hồ sơ cá nhân người dùng hiện tại — cho app cắt (F-07 / KX-04)")
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final UserDeviceService userDeviceService;

    public ProfileController(CurrentUserService currentUserService, UserDeviceService userDeviceService) {
        this.currentUserService = currentUserService;
        this.userDeviceService = userDeviceService;
    }

    @GetMapping
    @Operation(summary = "Hồ sơ của tôi",
            description = "User + đại lý + gói + hạn mức + thiết bị đang đăng nhập. Trường chưa có dữ liệu "
                    + "trả null/[] tường minh (null ≠ rỗng — KX-54), không trả 500.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hồ sơ hiện tại",
                    content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Thiếu/hỏng token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProfileResponse> myProfile() {
        User user = currentUserService.getCurrentUser();
        String sessionId = currentUserService.getCurrentJwt()
                .map(jwt -> jwt.getClaimAsString("sid"))
                .orElse(null);
        List<UserDeviceResponse> devices = userDeviceService.listDevices(user, sessionId);
        return ResponseEntity.ok(toProfile(user, devices));
    }

    private ProfileResponse toProfile(User user, List<UserDeviceResponse> devices) {
        Dealer dealer = user.getDealer();

        ProfileResponse.DealerBlock dealerBlock = dealer == null ? null
                : new ProfileResponse.DealerBlock(dealer.getId(), dealer.getCode(), dealer.getName());

        // Gói lấy nhãn trên hồ sơ đại lý (mô hình gói thật chờ C1/C10/C11). expiresAt để null:
        // dealers.due_date là chuỗi tự do ("12/2026", "—") không đổi được sang timestamp mà
        // không đoán định dạng — đoán sai là hiển thị sai hạn gói, tệ hơn để trống.
        ProfileResponse.PlanBlock planBlock = dealer == null || !StringUtils.hasText(dealer.getPlan()) ? null
                : new ProfileResponse.PlanBlock(dealer.getPlan(), null, null, null);

        String displayName = StringUtils.hasText(user.getFullName()) ? user.getFullName() : user.getUsername();
        ProfileResponse.UserBlock userBlock = new ProfileResponse.UserBlock(
                user.getId(), user.getUsername(), displayName,
                user.getRole() != null ? user.getRole().name() : null,
                user.getEmail(), user.getPhone());

        // quota chờ A6a — chưa có chỗ lưu nên trả null thay vì chuỗi bịa.
        return new ProfileResponse(userBlock, dealerBlock, planBlock, null, devices);
    }
}

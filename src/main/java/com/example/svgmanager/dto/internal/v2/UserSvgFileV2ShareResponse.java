package com.example.svgmanager.dto.internal.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Thông tin chi tiết một lượt chia sẻ file SVG V2")
public class UserSvgFileV2ShareResponse {

    @Schema(description = "ID của người dùng được chia sẻ", example = "2002")
    private Long userId;

    @Schema(description = "Tên đăng nhập của người dùng được chia sẻ", example = "user02")
    private String username;

    @Schema(description = "Tên hiển thị của người dùng được chia sẻ", example = "Nguyen Van B")
    private String displayName;

    @Schema(description = "ID đại lý của người dùng", example = "10")
    private Long dealerId;

    @Schema(description = "Tên đại lý của người dùng", example = "ABC")
    private String dealerName;

    @Schema(description = "Thời gian chia sẻ")
    private LocalDateTime sharedAt;

    @Schema(description = "Thông tin người thực hiện chia sẻ")
    private SharedByDto sharedBy;

    @Schema(description = "Trạng thái chia sẻ", example = "ACTIVE")
    private String status;

    public UserSvgFileV2ShareResponse() {
    }

    public static class SharedByDto {
        private Long userId;
        private String username;

        public SharedByDto() {}
        public SharedByDto(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Long getDealerId() { return dealerId; }
    public void setDealerId(Long dealerId) { this.dealerId = dealerId; }
    public String getDealerName() { return dealerName; }
    public void setDealerName(String dealerName) { this.dealerName = dealerName; }
    public LocalDateTime getSharedAt() { return sharedAt; }
    public void setSharedAt(LocalDateTime sharedAt) { this.sharedAt = sharedAt; }
    public SharedByDto getSharedBy() { return sharedBy; }
    public void setSharedBy(SharedByDto sharedBy) { this.sharedBy = sharedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

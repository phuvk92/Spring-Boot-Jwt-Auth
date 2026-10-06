package com.example.svgmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Yêu cầu chia sẻ file SVG")
public class ShareFileRequest {

    @NotNull(message = "userId không được để trống")
    @Schema(description = "ID của người dùng được chia sẻ quyền", example = "2002", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    public ShareFileRequest() {
    }

    public ShareFileRequest(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}

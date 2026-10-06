package com.example.svgmanager.dto.internal.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Yêu cầu chia sẻ file SVG V2")
public class UserSvgFileV2ShareRequest {

    @NotNull(message = "ID người dùng nhận chia sẻ không được để trống")
    @Schema(description = "ID của user nhận quyền chia sẻ file", example = "2002")
    private Long userId;

    public UserSvgFileV2ShareRequest() {
    }

    public UserSvgFileV2ShareRequest(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}

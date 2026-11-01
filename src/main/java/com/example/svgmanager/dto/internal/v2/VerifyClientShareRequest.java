package com.example.svgmanager.dto.internal.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request Client B xác thực mật khẩu lượt chia sẻ")
public class VerifyClientShareRequest {

    @NotBlank(message = "Mật khẩu không được để trống")
    @Schema(description = "Mật khẩu lượt chia sẻ", example = "Secret123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    public VerifyClientShareRequest() {
    }

    public VerifyClientShareRequest(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

package com.example.svgmanager.dto.internal.v2;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Kết quả xác thực lượt chia sẻ cho Client B")
public class VerifyClientShareResponse {

    @Schema(description = "Token xác thực lượt chia sẻ (dùng để preview và download)", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String shareToken;

    @Schema(description = "Loại token", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Thời gian hết hạn của token tính bằng giây", example = "3600")
    private long expiresIn;

    @Schema(description = "Mã lượt chia sẻ", example = "csh_8f91a2b3c4d5e6f7")
    private String shareId;

    @Schema(description = "ID bản lưu file SVG", example = "101")
    private Long fileId;

    @Schema(description = "Tên hiển thị file", example = "toyota_camry_door.svg")
    private String fileName;

    public VerifyClientShareResponse() {
    }

    public VerifyClientShareResponse(String shareToken, String tokenType, long expiresIn, String shareId, Long fileId, String fileName) {
        this.shareToken = shareToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.shareId = shareId;
        this.fileId = fileId;
        this.fileName = fileName;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}

package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Danh sách quyền chia sẻ của một file SVG")
public class FileSharesResponse {

    @Schema(description = "ID của file SVG", example = "1001")
    private Long fileId;

    @Schema(description = "Danh sách người dùng được chia sẻ file")
    private List<UserSvgFileShareResponse> shares = new ArrayList<>();

    public FileSharesResponse() {
    }

    public FileSharesResponse(Long fileId, List<UserSvgFileShareResponse> shares) {
        this.fileId = fileId;
        this.shares = shares != null ? shares : new ArrayList<>();
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public List<UserSvgFileShareResponse> getShares() {
        return shares;
    }

    public void setShares(List<UserSvgFileShareResponse> shares) {
        this.shares = shares != null ? shares : new ArrayList<>();
    }
}

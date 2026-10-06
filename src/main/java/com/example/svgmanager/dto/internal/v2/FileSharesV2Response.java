package com.example.svgmanager.dto.internal.v2;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Danh sách quyền chia sẻ của một file SVG V2")
public class FileSharesV2Response {

    @Schema(description = "ID của file SVG V2", example = "1001")
    private Long fileId;

    @Schema(description = "Danh sách người dùng được chia sẻ file")
    private List<UserSvgFileV2ShareResponse> shares = new ArrayList<>();

    public FileSharesV2Response() {
    }

    public FileSharesV2Response(Long fileId, List<UserSvgFileV2ShareResponse> shares) {
        this.fileId = fileId;
        this.shares = shares != null ? shares : new ArrayList<>();
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public List<UserSvgFileV2ShareResponse> getShares() {
        return shares;
    }

    public void setShares(List<UserSvgFileV2ShareResponse> shares) {
        this.shares = shares != null ? shares : new ArrayList<>();
    }
}

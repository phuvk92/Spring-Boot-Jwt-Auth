package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "SVG file metadata response")
public class SvgResponse {

    @Schema(description = "Unique SVG file ID", example = "1")
    private Long id;

    @Schema(description = "Original uploaded filename", example = "logo.svg")
    private String originalFilename;

    @Schema(description = "Unique internal stored filename", example = "d3b07384-d113-40a2-a9a3-a0e28f323c68.svg")
    private String storedFilename;

    @Schema(description = "File size in bytes", example = "15360")
    private Long fileSize;

    @Schema(description = "MIME content type", example = "image/svg+xml")
    private String contentType;

    @Schema(description = "SHA-256 checksum of sanitized content", example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    private String checksum;

    @Schema(description = "Status of SVG file", example = "ACTIVE")
    private String status;

    @Schema(description = "File category name (Ngoại thất, Window film…)", example = "Ngoại thất")
    private String fileCategory;

    @Schema(description = "Năm xe áp dụng; null = mọi năm", example = "2024")
    private Integer modelYear;

    @Schema(description = "Nguồn file: SYSTEM | DEALER", example = "SYSTEM")
    private String source;

    @Schema(description = "Whether current user is authorized to download this file", example = "true")
    private Boolean canDownload;

    @Schema(description = "Whether current user is authorized to view this file", example = "true")
    private Boolean canView;

    @Schema(description = "User who uploaded the SVG")
    private UserSummaryResponse uploadedBy;

    @Schema(description = "Agent ID associated with this SVG", example = "2")
    private Long agentId;

    @Schema(description = "Upload timestamp", example = "2026-03-30T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2026-03-30T10:00:00")
    private LocalDateTime updatedAt;

    public SvgResponse() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SvgResponse r = new SvgResponse();

        public Builder id(Long v) { r.id = v; return this; }
        public Builder originalFilename(String v) { r.originalFilename = v; return this; }
        public Builder storedFilename(String v) { r.storedFilename = v; return this; }
        public Builder fileSize(Long v) { r.fileSize = v; return this; }
        public Builder contentType(String v) { r.contentType = v; return this; }
        public Builder checksum(String v) { r.checksum = v; return this; }
        public Builder status(String v) { r.status = v; return this; }
        public Builder fileCategory(String v) { r.fileCategory = v; return this; }
        public Builder modelYear(Integer v) { r.modelYear = v; return this; }
        public Builder source(String v) { r.source = v; return this; }
        public Builder canDownload(Boolean v) { r.canDownload = v; return this; }
        public Builder canView(Boolean v) { r.canView = v; return this; }
        public Builder uploadedBy(UserSummaryResponse v) { r.uploadedBy = v; return this; }
        public Builder agentId(Long v) { r.agentId = v; return this; }
        public Builder createdAt(LocalDateTime v) { r.createdAt = v; return this; }
        public Builder updatedAt(LocalDateTime v) { r.updatedAt = v; return this; }

        public SvgResponse build() {
            return r;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFileCategory() {
        return fileCategory;
    }

    public void setFileCategory(String fileCategory) {
        this.fileCategory = fileCategory;
    }

    public Integer getModelYear() {
        return modelYear;
    }

    public void setModelYear(Integer modelYear) {
        this.modelYear = modelYear;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Boolean getCanDownload() {
        return canDownload;
    }

    public void setCanDownload(Boolean canDownload) {
        this.canDownload = canDownload;
    }

    public Boolean getCanView() {
        return canView;
    }

    public void setCanView(Boolean canView) {
        this.canView = canView;
    }

    public UserSummaryResponse getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UserSummaryResponse uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

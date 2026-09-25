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

    @Schema(description = "Assigned Category metadata")
    private CategorySummaryResponse category;

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

    public SvgResponse(Long id, String originalFilename, String storedFilename, Long fileSize, String contentType, String checksum, CategorySummaryResponse category, UserSummaryResponse uploadedBy, Long agentId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.checksum = checksum;
        this.category = category;
        this.uploadedBy = uploadedBy;
        this.agentId = agentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String originalFilename;
        private String storedFilename;
        private Long fileSize;
        private String contentType;
        private String checksum;
        private CategorySummaryResponse category;
        private UserSummaryResponse uploadedBy;
        private Long agentId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder originalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
            return this;
        }

        public Builder storedFilename(String storedFilename) {
            this.storedFilename = storedFilename;
            return this;
        }

        public Builder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder checksum(String checksum) {
            this.checksum = checksum;
            return this;
        }

        public Builder category(CategorySummaryResponse category) {
            this.category = category;
            return this;
        }

        public Builder uploadedBy(UserSummaryResponse uploadedBy) {
            this.uploadedBy = uploadedBy;
            return this;
        }

        public Builder agentId(Long agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public SvgResponse build() {
            return new SvgResponse(id, originalFilename, storedFilename, fileSize, contentType, checksum, category, uploadedBy, agentId, createdAt, updatedAt);
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

    public CategorySummaryResponse getCategory() {
        return category;
    }

    public void setCategory(CategorySummaryResponse category) {
        this.category = category;
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

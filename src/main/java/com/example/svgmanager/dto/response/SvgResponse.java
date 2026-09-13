package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "SVG file metadata response")
public class SvgResponse {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "logo.svg")
    private String originalFilename;

    @Schema(example = "102400", description = "File size in bytes")
    private Long fileSize;

    @Schema(example = "image/svg+xml")
    private String contentType;

    @Schema(example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", description = "SHA-256 Checksum")
    private String checksum;

    @Schema(description = "Information of user who uploaded the SVG")
    private UserSummaryResponse uploadedBy;

    @Schema(example = "2026-09-13T10:00:00")
    private LocalDateTime createdAt;

    @Schema(example = "2026-09-13T10:00:00")
    private LocalDateTime updatedAt;

    public SvgResponse() {
    }

    public SvgResponse(Long id, String originalFilename, Long fileSize, String contentType, String checksum, UserSummaryResponse uploadedBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.checksum = checksum;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String originalFilename;
        private Long fileSize;
        private String contentType;
        private String checksum;
        private UserSummaryResponse uploadedBy;
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

        public Builder uploadedBy(UserSummaryResponse uploadedBy) {
            this.uploadedBy = uploadedBy;
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
            return new SvgResponse(id, originalFilename, fileSize, contentType, checksum, uploadedBy, createdAt, updatedAt);
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

    public UserSummaryResponse getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UserSummaryResponse uploadedBy) {
        this.uploadedBy = uploadedBy;
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

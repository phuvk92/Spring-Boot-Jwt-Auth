package com.example.svgmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Schema(description = "Assigned Category metadata")
    private CategorySummaryResponse category;

    @Schema(description = "Assigned Vehicle Configurations")
    private List<VehicleConfigurationResponse> vehicleConfigurations = new ArrayList<>();

    @Schema(description = "Number of dealers with active view permissions", example = "3")
    private int dealerPermissionCount;

    @Schema(description = "Detailed dealer permissions (ADMIN only)")
    private List<SvgFileDealerPermissionResponse> dealerPermissions;

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

    public SvgResponse(Long id, String originalFilename, String storedFilename, Long fileSize,
                       String contentType, String checksum, String status, CategorySummaryResponse category,
                       List<VehicleConfigurationResponse> vehicleConfigurations, int dealerPermissionCount,
                       List<SvgFileDealerPermissionResponse> dealerPermissions, Boolean canDownload,
                       Boolean canView, UserSummaryResponse uploadedBy, Long agentId,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.checksum = checksum;
        this.status = status;
        this.category = category;
        this.vehicleConfigurations = vehicleConfigurations != null ? vehicleConfigurations : new ArrayList<>();
        this.dealerPermissionCount = dealerPermissionCount;
        this.dealerPermissions = dealerPermissions;
        this.canDownload = canDownload;
        this.canView = canView;
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
        private String status;
        private CategorySummaryResponse category;
        private List<VehicleConfigurationResponse> vehicleConfigurations = new ArrayList<>();
        private int dealerPermissionCount;
        private List<SvgFileDealerPermissionResponse> dealerPermissions;
        private Boolean canDownload;
        private Boolean canView;
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

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder category(CategorySummaryResponse category) {
            this.category = category;
            return this;
        }

        public Builder vehicleConfigurations(List<VehicleConfigurationResponse> vehicleConfigurations) {
            this.vehicleConfigurations = vehicleConfigurations;
            return this;
        }

        public Builder dealerPermissionCount(int dealerPermissionCount) {
            this.dealerPermissionCount = dealerPermissionCount;
            return this;
        }

        public Builder dealerPermissions(List<SvgFileDealerPermissionResponse> dealerPermissions) {
            this.dealerPermissions = dealerPermissions;
            return this;
        }

        public Builder canDownload(Boolean canDownload) {
            this.canDownload = canDownload;
            return this;
        }

        public Builder canView(Boolean canView) {
            this.canView = canView;
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
            return new SvgResponse(id, originalFilename, storedFilename, fileSize, contentType, checksum, status,
                    category, vehicleConfigurations, dealerPermissionCount, dealerPermissions, canDownload, canView,
                    uploadedBy, agentId, createdAt, updatedAt);
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

    public CategorySummaryResponse getCategory() {
        return category;
    }

    public void setCategory(CategorySummaryResponse category) {
        this.category = category;
    }

    public List<VehicleConfigurationResponse> getVehicleConfigurations() {
        return vehicleConfigurations;
    }

    public void setVehicleConfigurations(List<VehicleConfigurationResponse> vehicleConfigurations) {
        this.vehicleConfigurations = vehicleConfigurations;
    }

    public int getDealerPermissionCount() {
        return dealerPermissionCount;
    }

    public void setDealerPermissionCount(int dealerPermissionCount) {
        this.dealerPermissionCount = dealerPermissionCount;
    }

    public List<SvgFileDealerPermissionResponse> getDealerPermissions() {
        return dealerPermissions;
    }

    public void setDealerPermissions(List<SvgFileDealerPermissionResponse> dealerPermissions) {
        this.dealerPermissions = dealerPermissions;
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

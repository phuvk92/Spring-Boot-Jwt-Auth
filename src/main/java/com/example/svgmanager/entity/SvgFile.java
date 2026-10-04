package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "svg_files", indexes = {
        @Index(name = "idx_svg_files_status", columnList = "status"),
        @Index(name = "idx_svg_files_checksum", columnList = "checksum"),
        @Index(name = "idx_svg_original_filename", columnList = "original_filename"),
        @Index(name = "idx_svg_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class SvgFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", unique = true, length = 255)
    private String storedFilename;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "checksum", length = 128)
    private String checksum;

    // ---- Bản chưa xếp (RAW) — SA-DanhMucXe-v2 §8 (NGO-378) ----------------------
    @Column(name = "raw_stored_filename", length = 255)
    private String rawStoredFilename;

    @Column(name = "raw_original_filename", length = 255)
    private String rawOriginalFilename;

    @Column(name = "raw_file_path", length = 1000)
    private String rawFilePath;

    @Column(name = "raw_file_size")
    private Long rawFileSize;

    @Column(name = "raw_checksum", length = 128)
    private String rawChecksum;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";

    /** Danh mục file (Ngoại thất, Window film…) — NULL chỉ với file cũ chưa gắn. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_category_id")
    private FileCategory fileCategory;

    /** Danh mục kho mẫu & part (PartLibraryCategory) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_library_category_id")
    private PartLibraryCategory partLibraryCategory;

    /** Năm xe áp dụng; NULL = hiện với mọi năm khi thợ lọc (Q3). Cột SMALLINT trong DB. */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "model_year")
    private Integer modelYear;

    /** Ảnh xem trước tuỳ chọn (đường dẫn trong storage). */
    @Column(name = "thumbnail_path", length = 1000)
    private String thumbnailPath;

    /** Nguồn file: SYSTEM | DEALER — chỉ hiển thị, không phân quyền. */
    @Column(name = "source", nullable = false, length = 10)
    private String source = "SYSTEM";

    /** Các node xe (MODEL/SUBTYPE) file gắn vào — N:N, Q5. */
    @OneToMany(mappedBy = "svgFile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SvgFileVehicleNode> vehicleNodes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User agent;

    /** Id chuỗi trong hợp đồng /api/v1/files (slug kèm khoá xe) — đội nội dung đặt, duy nhất. */
    @Column(name = "file_key", unique = true, length = 255)
    private String fileKey;

    /** Tên hiển thị do đội nội dung đặt (vd 'Ngoại thất — full body 7 mảnh'), khác tên file vật lý. */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /** Tổng phim cả file — chuỗi nguyên văn kèm đơn vị ('6,46 m'), server không tự tính. */
    @Column(name = "film_usage", length = 100)
    private String filmUsage;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** Khổ cắt khai theo file (epic NGO-399): chiều dọc cuộn, trục X — mm, 100–50000. NULL khi chưa khai. */
    @Column(name = "cut_area_length_mm")
    private Integer cutAreaLengthMm;

    /** Khổ phim, trục Y — mm, 100–2000. Hai cột khổ cắt cùng NULL hoặc cùng có giá trị (chk_svg_files_cut_area). */
    @Column(name = "cut_area_width_mm")
    private Integer cutAreaWidthMm;

    @OneToMany(mappedBy = "svgFile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<SvgFilePart> parts = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SvgFile() {
    }

    public SvgFile(Long id, String originalFilename, String storedFilename, String filePath, Long fileSize,
                   String contentType, String checksum, String status, User uploadedBy, User agent,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.checksum = checksum;
        this.status = status != null ? status : "ACTIVE";
        this.uploadedBy = uploadedBy;
        this.agent = agent;
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
        private String filePath;
        private Long fileSize;
        private String contentType;
        private String checksum;
        private String status = "ACTIVE";
        private User uploadedBy;
        private User agent;
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

        public Builder filePath(String filePath) {
            this.filePath = filePath;
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

        public Builder uploadedBy(User uploadedBy) {
            this.uploadedBy = uploadedBy;
            return this;
        }

        public Builder agent(User agent) {
            this.agent = agent;
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

        public SvgFile build() {
            return new SvgFile(id, originalFilename, storedFilename, filePath, fileSize, contentType, checksum, status, uploadedBy, agent, createdAt, updatedAt);
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

    public FileCategory getFileCategory() {
        return fileCategory;
    }

    public void setFileCategory(FileCategory fileCategory) {
        this.fileCategory = fileCategory;
    }

    public PartLibraryCategory getPartLibraryCategory() {
        return partLibraryCategory;
    }

    public void setPartLibraryCategory(PartLibraryCategory partLibraryCategory) {
        this.partLibraryCategory = partLibraryCategory;
    }

    public Integer getModelYear() {
        return modelYear;
    }

    public void setModelYear(Integer modelYear) {
        this.modelYear = modelYear;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<SvgFileVehicleNode> getVehicleNodes() {
        return vehicleNodes;
    }

    public void setVehicleNodes(List<SvgFileVehicleNode> vehicleNodes) {
        this.vehicleNodes = vehicleNodes != null ? vehicleNodes : new ArrayList<>();
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public User getAgent() {
        return agent;
    }

    public void setAgent(User agent) {
        this.agent = agent;
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

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getFilmUsage() {
        return filmUsage;
    }

    public void setFilmUsage(String filmUsage) {
        this.filmUsage = filmUsage;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getCutAreaLengthMm() {
        return cutAreaLengthMm;
    }

    public void setCutAreaLengthMm(Integer cutAreaLengthMm) {
        this.cutAreaLengthMm = cutAreaLengthMm;
    }

    public Integer getCutAreaWidthMm() {
        return cutAreaWidthMm;
    }

    public void setCutAreaWidthMm(Integer cutAreaWidthMm) {
        this.cutAreaWidthMm = cutAreaWidthMm;
    }

    public List<SvgFilePart> getParts() {
        return parts;
    }

    public void setParts(List<SvgFilePart> parts) {
        this.parts = parts != null ? parts : new ArrayList<>();
    }

    public boolean hasNested() {
        return (this.storedFilename != null && !this.storedFilename.isBlank())
                || (this.filePath != null && !this.filePath.isBlank());
    }

    public boolean hasRaw() {
        return (this.rawStoredFilename != null && !this.rawStoredFilename.isBlank())
                || (this.rawFilePath != null && !this.rawFilePath.isBlank());
    }

    public String getRawStoredFilename() {
        return rawStoredFilename;
    }

    public void setRawStoredFilename(String rawStoredFilename) {
        this.rawStoredFilename = rawStoredFilename;
    }

    public String getRawOriginalFilename() {
        return rawOriginalFilename;
    }

    public void setRawOriginalFilename(String rawOriginalFilename) {
        this.rawOriginalFilename = rawOriginalFilename;
    }

    public String getRawFilePath() {
        return rawFilePath;
    }

    public void setRawFilePath(String rawFilePath) {
        this.rawFilePath = rawFilePath;
    }

    public Long getRawFileSize() {
        return rawFileSize;
    }

    public void setRawFileSize(Long rawFileSize) {
        this.rawFileSize = rawFileSize;
    }

    public String getRawChecksum() {
        return rawChecksum;
    }

    public void setRawChecksum(String rawChecksum) {
        this.rawChecksum = rawChecksum;
    }
}

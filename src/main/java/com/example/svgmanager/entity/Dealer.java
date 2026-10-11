package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dealers", indexes = {
        @Index(name = "idx_dealers_code", columnList = "code"),
        @Index(name = "idx_dealers_name", columnList = "name"),
        @Index(name = "idx_dealers_region", columnList = "region"),
        @Index(name = "idx_dealers_status", columnList = "status"),
        @Index(name = "idx_dealers_deleted", columnList = "deleted"),
        @Index(name = "idx_dealers_pkg_status", columnList = "package_status"),
        @Index(name = "idx_dealers_pkg_expiry", columnList = "package_expiry_date")
})
@EntityListeners(AuditingEntityListener.class)
public class Dealer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 100)
    private String region;

    @Column(length = 255)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(nullable = false, length = 50)
    private String plan = "Cơ bản";

    @Column(name = "due_date", length = 50)
    private String dueDate;

    @Column(nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean deleted = false;

    // --- Thông tin gói account của đại lý ---
    @Column(name = "max_accounts", nullable = false)
    private Integer maxAccounts = 10;

    @Column(name = "total_allocated_days", nullable = false)
    private Integer totalAllocatedDays = 3600;

    @Column(name = "used_allocated_days", nullable = false)
    private Integer usedAllocatedDays = 0;

    @Column(name = "package_start_date")
    private LocalDate packageStartDate;

    @Column(name = "package_expiry_date")
    private LocalDate packageExpiryDate;

    @Column(name = "package_status", nullable = false, length = 50)
    private String packageStatus = "ACTIVE";

    @OneToMany(mappedBy = "dealer", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Dealer() {
    }

    public Dealer(Long id, String code, String name, String region, String address, String phone,
                  String email, String contactPerson, String plan, String dueDate, String status,
                  String notes, boolean deleted, Integer maxAccounts, Integer totalAllocatedDays,
                  Integer usedAllocatedDays, LocalDate packageStartDate, LocalDate packageExpiryDate,
                  String packageStatus, List<User> users, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.region = region;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.contactPerson = contactPerson;
        this.plan = plan != null ? plan : "Cơ bản";
        this.dueDate = dueDate;
        this.status = status != null ? status : "ACTIVE";
        this.notes = notes;
        this.deleted = deleted;
        this.maxAccounts = maxAccounts != null ? maxAccounts : 10;
        this.totalAllocatedDays = totalAllocatedDays != null ? totalAllocatedDays : 3600;
        this.usedAllocatedDays = usedAllocatedDays != null ? usedAllocatedDays : 0;
        this.packageStartDate = packageStartDate;
        this.packageExpiryDate = packageExpiryDate;
        this.packageStatus = packageStatus != null ? packageStatus : "ACTIVE";
        this.users = users != null ? users : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getRemainingAllocatedDays() {
        int total = totalAllocatedDays != null ? totalAllocatedDays : 0;
        int used = usedAllocatedDays != null ? usedAllocatedDays : 0;
        return Math.max(0, total - used);
    }

    public boolean isPackageExpired() {
        return packageExpiryDate != null && LocalDate.now().isAfter(packageExpiryDate);
    }

    public boolean isPackageActive() {
        return "ACTIVE".equalsIgnoreCase(packageStatus) && !isPackageExpired() && !deleted;
    }

    public static class Builder {
        private Long id;
        private String code;
        private String name;
        private String region;
        private String address;
        private String phone;
        private String email;
        private String contactPerson;
        private String plan = "Cơ bản";
        private String dueDate;
        private String status = "ACTIVE";
        private String notes;
        private boolean deleted = false;
        private Integer maxAccounts = 10;
        private Integer totalAllocatedDays = 3600;
        private Integer usedAllocatedDays = 0;
        private LocalDate packageStartDate;
        private LocalDate packageExpiryDate;
        private String packageStatus = "ACTIVE";
        private List<User> users = new ArrayList<>();
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder contactPerson(String contactPerson) {
            this.contactPerson = contactPerson;
            return this;
        }

        public Builder plan(String plan) {
            this.plan = plan;
            return this;
        }

        public Builder dueDate(String dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder deleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Builder maxAccounts(Integer maxAccounts) {
            this.maxAccounts = maxAccounts;
            return this;
        }

        public Builder totalAllocatedDays(Integer totalAllocatedDays) {
            this.totalAllocatedDays = totalAllocatedDays;
            return this;
        }

        public Builder usedAllocatedDays(Integer usedAllocatedDays) {
            this.usedAllocatedDays = usedAllocatedDays;
            return this;
        }

        public Builder packageStartDate(LocalDate packageStartDate) {
            this.packageStartDate = packageStartDate;
            return this;
        }

        public Builder packageExpiryDate(LocalDate packageExpiryDate) {
            this.packageExpiryDate = packageExpiryDate;
            return this;
        }

        public Builder packageStatus(String packageStatus) {
            this.packageStatus = packageStatus;
            return this;
        }

        public Builder users(List<User> users) {
            this.users = users;
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

        public Dealer build() {
            return new Dealer(id, code, name, region, address, phone, email, contactPerson,
                    plan, dueDate, status, notes, deleted, maxAccounts, totalAllocatedDays,
                    usedAllocatedDays, packageStartDate, packageExpiryDate, packageStatus,
                    users, createdAt, updatedAt);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Integer getMaxAccounts() {
        return maxAccounts;
    }

    public void setMaxAccounts(Integer maxAccounts) {
        this.maxAccounts = maxAccounts;
    }

    public Integer getTotalAllocatedDays() {
        return totalAllocatedDays;
    }

    public void setTotalAllocatedDays(Integer totalAllocatedDays) {
        this.totalAllocatedDays = totalAllocatedDays;
    }

    public Integer getUsedAllocatedDays() {
        return usedAllocatedDays;
    }

    public void setUsedAllocatedDays(Integer usedAllocatedDays) {
        this.usedAllocatedDays = usedAllocatedDays;
    }

    public LocalDate getPackageStartDate() {
        return packageStartDate;
    }

    public void setPackageStartDate(LocalDate packageStartDate) {
        this.packageStartDate = packageStartDate;
    }

    public LocalDate getPackageExpiryDate() {
        return packageExpiryDate;
    }

    public void setPackageExpiryDate(LocalDate packageExpiryDate) {
        this.packageExpiryDate = packageExpiryDate;
    }

    public String getPackageStatus() {
        return packageStatus;
    }

    public void setPackageStatus(String packageStatus) {
        this.packageStatus = packageStatus;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
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

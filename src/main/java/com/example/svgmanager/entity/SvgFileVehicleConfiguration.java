package com.example.svgmanager.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "svg_file_vehicle_configurations", uniqueConstraints = {
        @UniqueConstraint(name = "uq_svg_vehicle_config", columnNames = {"svg_file_id", "vehicle_configuration_id"})
}, indexes = {
        @Index(name = "idx_svg_vc_svg_file_id", columnList = "svg_file_id"),
        @Index(name = "idx_svg_vc_config_id", columnList = "vehicle_configuration_id")
})
@EntityListeners(AuditingEntityListener.class)
public class SvgFileVehicleConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "svg_file_id", nullable = false)
    private SvgFile svgFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_configuration_id", nullable = false)
    private VehicleConfiguration vehicleConfiguration;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SvgFileVehicleConfiguration() {
    }

    public SvgFileVehicleConfiguration(Long id, SvgFile svgFile, VehicleConfiguration vehicleConfiguration, LocalDateTime createdAt) {
        this.id = id;
        this.svgFile = svgFile;
        this.vehicleConfiguration = vehicleConfiguration;
        this.createdAt = createdAt;
    }

    public SvgFileVehicleConfiguration(SvgFile svgFile, VehicleConfiguration vehicleConfiguration) {
        this.svgFile = svgFile;
        this.vehicleConfiguration = vehicleConfiguration;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SvgFile getSvgFile() {
        return svgFile;
    }

    public void setSvgFile(SvgFile svgFile) {
        this.svgFile = svgFile;
    }

    public VehicleConfiguration getVehicleConfiguration() {
        return vehicleConfiguration;
    }

    public void setVehicleConfiguration(VehicleConfiguration vehicleConfiguration) {
        this.vehicleConfiguration = vehicleConfiguration;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

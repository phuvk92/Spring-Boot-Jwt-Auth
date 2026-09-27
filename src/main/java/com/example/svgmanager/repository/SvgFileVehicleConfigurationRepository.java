package com.example.svgmanager.repository;

import com.example.svgmanager.entity.SvgFileVehicleConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SvgFileVehicleConfigurationRepository extends JpaRepository<SvgFileVehicleConfiguration, Long> {

    List<SvgFileVehicleConfiguration> findBySvgFileId(Long svgFileId);

    boolean existsBySvgFileIdAndVehicleConfigurationId(Long svgFileId, Long vehicleConfigurationId);

    @Modifying
    @Query("DELETE FROM SvgFileVehicleConfiguration svc WHERE svc.svgFile.id = :svgFileId")
    void deleteBySvgFileId(@Param("svgFileId") Long svgFileId);

    @Modifying
    @Query("DELETE FROM SvgFileVehicleConfiguration svc WHERE svc.svgFile.id = :svgFileId AND svc.vehicleConfiguration.id = :configId")
    void deleteBySvgFileIdAndVehicleConfigurationId(@Param("svgFileId") Long svgFileId, @Param("configId") Long configId);
}

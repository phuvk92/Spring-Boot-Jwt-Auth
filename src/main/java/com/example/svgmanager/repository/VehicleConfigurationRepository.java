package com.example.svgmanager.repository;

import com.example.svgmanager.entity.VehicleConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleConfigurationRepository extends JpaRepository<VehicleConfiguration, Long>, JpaSpecificationExecutor<VehicleConfiguration> {

    Optional<VehicleConfiguration> findByIdAndDeletedFalse(Long id);

    boolean existsByCategoryIdAndBrandIdAndModelIdAndYearFromAndYearToAndGenerationCodeIgnoreCaseAndDeletedFalse(
            Long categoryId,
            Long brandId,
            Long modelId,
            Integer yearFrom,
            Integer yearTo,
            String generationCode
    );

    @Query("SELECT CASE WHEN COUNT(vc) > 0 THEN true ELSE false END FROM VehicleConfiguration vc " +
            "WHERE vc.category.id = :categoryId AND vc.brand.id = :brandId AND vc.model.id = :modelId " +
            "AND vc.yearFrom = :yearFrom AND vc.yearTo = :yearTo " +
            "AND LOWER(vc.generationCode) = LOWER(:generationCode) " +
            "AND vc.deleted = false AND vc.id <> :excludeId")
    boolean existsDuplicateExcludingId(
            @Param("categoryId") Long categoryId,
            @Param("brandId") Long brandId,
            @Param("modelId") Long modelId,
            @Param("yearFrom") Integer yearFrom,
            @Param("yearTo") Integer yearTo,
            @Param("generationCode") String generationCode,
            @Param("excludeId") Long excludeId
    );

    boolean existsByBrandIdAndDeletedFalse(Long brandId);

    boolean existsByModelIdAndDeletedFalse(Long modelId);

    boolean existsByCategoryIdAndDeletedFalse(Long categoryId);
}

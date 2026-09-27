package com.example.svgmanager.repository;

import com.example.svgmanager.entity.CarModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarModelRepository extends JpaRepository<CarModel, Long> {
    List<CarModel> findByBrandIdAndStatusOrderByDisplayOrderAscNameAsc(Long brandId, String status);
    List<CarModel> findByBrandIdOrderByDisplayOrderAscNameAsc(Long brandId);
    Optional<CarModel> findByBrandIdAndCodeIgnoreCase(Long brandId, String code);
    boolean existsByBrandIdAndCodeIgnoreCase(Long brandId, String code);
    boolean existsByBrandId(Long brandId);
    Page<CarModel> findByBrandId(Long brandId, Pageable pageable);
}

package com.example.svgmanager.repository;

import com.example.svgmanager.entity.CarBrand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarBrandRepository extends JpaRepository<CarBrand, Long> {
    Optional<CarBrand> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<CarBrand> findByStatusOrderByDisplayOrderAscNameAsc(String status);
    Page<CarBrand> findByStatus(String status, Pageable pageable);
}

package com.example.svgmanager.repository;

import com.example.svgmanager.entity.PartLibraryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartLibraryCategoryRepository extends JpaRepository<PartLibraryCategory, Long>, JpaSpecificationExecutor<PartLibraryCategory> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    Optional<PartLibraryCategory> findByCode(String code);

    List<PartLibraryCategory> findByStatusOrderByNameAsc(String status);

    List<PartLibraryCategory> findAllByOrderByNameAsc();
}

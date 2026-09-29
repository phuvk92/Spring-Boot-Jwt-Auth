package com.example.svgmanager.repository;

import com.example.svgmanager.entity.FileCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileCategoryRepository extends JpaRepository<FileCategory, Long> {

    List<FileCategory> findByActiveTrueOrderByDisplayOrderAscIdAsc();
}

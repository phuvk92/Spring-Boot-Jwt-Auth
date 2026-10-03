package com.example.svgmanager.repository;

import com.example.svgmanager.entity.UserSvgFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSvgFileRepository extends JpaRepository<UserSvgFile, Long>, JpaSpecificationExecutor<UserSvgFile> {

    Optional<UserSvgFile> findByIdAndUserId(Long id, Long userId);

    Page<UserSvgFile> findByUserId(Long userId, Pageable pageable);
}

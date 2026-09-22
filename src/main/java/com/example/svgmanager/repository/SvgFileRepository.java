package com.example.svgmanager.repository;

import com.example.svgmanager.entity.SvgFile;
import com.example.svgmanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SvgFileRepository extends JpaRepository<SvgFile, Long>, JpaSpecificationExecutor<SvgFile> {

    Optional<SvgFile> findByStoredFilename(String storedFilename);

    Optional<SvgFile> findByIdAndAgentId(Long id, Long agentId);

    Page<SvgFile> findByAgentId(Long agentId, Pageable pageable);

    boolean existsByUploadedBy(User user);

    long countByUploadedBy(User user);

    boolean existsByAgent(User agent);
}

package com.example.svgmanager.repository;

import com.example.svgmanager.entity.SvgFilePart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SvgFilePartRepository extends JpaRepository<SvgFilePart, Long> {

    List<SvgFilePart> findBySvgFileIdOrderByDisplayOrderAscIdAsc(Long svgFileId);

    long countBySvgFileId(Long svgFileId);
}

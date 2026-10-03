package com.example.svgmanager.repository;

import com.example.svgmanager.entity.WorkDesignVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkDesignVersionRepository extends JpaRepository<WorkDesignVersion, Long> {

    /** Panel phiên bản (F-37) — bản mới nhất trước. */
    List<WorkDesignVersion> findByWorkDesignIdOrderByNumberDesc(Long workDesignId);

    /** versionCount của SavedDesign — gom một query cho cả trang, tránh N+1. */
    @Query("select v.workDesign.id, count(v) from WorkDesignVersion v where v.workDesign.id in :ids group by v.workDesign.id")
    List<Object[]> countGroupedByWorkDesignIds(@Param("ids") Collection<Long> ids);

    Optional<WorkDesignVersion> findByWorkDesignIdAndNumber(Long workDesignId, int number);

    Optional<WorkDesignVersion> findByWorkDesignIdAndCurrentTrue(Long workDesignId);
}

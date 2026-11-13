package com.example.svgmanager.repository;

import com.example.svgmanager.entity.CutJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CutJobRepository extends JpaRepository<CutJob, Long> {

    List<CutJob> findByUserDeviceIdOrderByCutAtDesc(Long userDeviceId);

    Optional<CutJob> findByIdAndUserDeviceId(Long id, Long userDeviceId);

    Optional<CutJob> findByIdAndDealerId(Long id, Long dealerId);

    Optional<CutJob> findByDealerIdAndIdempotencyKey(Long dealerId, String idempotencyKey);

    boolean existsByUserDeviceIdAndDesignId(Long userDeviceId, String designId);
}

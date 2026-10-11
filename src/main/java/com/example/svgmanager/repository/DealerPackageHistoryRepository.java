package com.example.svgmanager.repository;

import com.example.svgmanager.entity.DealerPackageHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DealerPackageHistoryRepository extends JpaRepository<DealerPackageHistory, Long> {

    List<DealerPackageHistory> findByDealerIdOrderByCreatedAtDesc(Long dealerId);

    Page<DealerPackageHistory> findByDealerIdOrderByCreatedAtDesc(Long dealerId, Pageable pageable);
}

package com.example.svgmanager.repository;

import com.example.svgmanager.entity.DealerBranding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DealerBrandingRepository extends JpaRepository<DealerBranding, Long> {
}

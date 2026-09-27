package com.example.svgmanager.repository;

import com.example.svgmanager.entity.Dealer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DealerRepository extends JpaRepository<Dealer, Long>, JpaSpecificationExecutor<Dealer> {

    Optional<Dealer> findByIdAndDeletedFalse(Long id);

    Optional<Dealer> findByCodeAndDeletedFalse(String code);

    boolean existsByCodeAndDeletedFalse(String code);

    boolean existsByCodeAndIdNotAndDeletedFalse(String code, Long id);

    List<Dealer> findAllByDeletedFalseOrderByCreatedAtDesc();

    @Query("SELECT COUNT(u) FROM User u WHERE u.dealer.id = :dealerId AND u.deleted = false")
    long countUsersByDealerId(@Param("dealerId") Long dealerId);

    long countByDeletedFalse();

    long countByDeletedFalseAndStatus(String status);
}

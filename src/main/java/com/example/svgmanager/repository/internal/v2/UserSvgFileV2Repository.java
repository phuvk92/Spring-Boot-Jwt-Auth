package com.example.svgmanager.repository.internal.v2;

import com.example.svgmanager.entity.v2.UserSvgFileV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSvgFileV2Repository extends JpaRepository<UserSvgFileV2, Long>, JpaSpecificationExecutor<UserSvgFileV2> {

    Optional<UserSvgFileV2> findByIdAndUserId(Long id, Long userId);
}

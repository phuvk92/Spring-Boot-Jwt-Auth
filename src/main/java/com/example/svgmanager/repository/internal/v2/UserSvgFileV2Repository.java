package com.example.svgmanager.repository.internal.v2;

import com.example.svgmanager.entity.v2.UserSvgFileV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSvgFileV2Repository extends JpaRepository<UserSvgFileV2, Long>, JpaSpecificationExecutor<UserSvgFileV2> {

    Optional<UserSvgFileV2> findByIdAndUserId(Long id, Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT f FROM UserSvgFileV2 f JOIN FETCH f.user WHERE f.id = :id")
    Optional<UserSvgFileV2> findWithUserById(@org.springframework.data.repository.query.Param("id") Long id);
}

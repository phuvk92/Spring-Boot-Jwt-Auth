package com.example.svgmanager.repository;

import com.example.svgmanager.entity.UserSvgFileShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSvgFileShareRepository extends JpaRepository<UserSvgFileShare, Long> {

    List<UserSvgFileShare> findByUserSvgFileIdAndStatus(Long userSvgFileId, String status);

    Optional<UserSvgFileShare> findByUserSvgFileIdAndSharedToUserId(Long userSvgFileId, Long sharedToUserId);

    Optional<UserSvgFileShare> findByUserSvgFileIdAndSharedToUserIdAndStatus(Long userSvgFileId, Long sharedToUserId, String status);

    boolean existsByUserSvgFileIdAndSharedToUserIdAndStatus(Long userSvgFileId, Long sharedToUserId, String status);

    @Query("SELECT s.userSvgFile.id FROM UserSvgFileShare s WHERE s.sharedToUser.id = :userId AND s.status = 'ACTIVE'")
    List<Long> findActiveSharedFileIdsByUserId(@Param("userId") Long userId);
}

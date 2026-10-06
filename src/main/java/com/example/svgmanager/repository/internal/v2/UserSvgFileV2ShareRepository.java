package com.example.svgmanager.repository.internal.v2;

import com.example.svgmanager.entity.v2.UserSvgFileV2Share;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSvgFileV2ShareRepository extends JpaRepository<UserSvgFileV2Share, Long> {

    Optional<UserSvgFileV2Share> findByUserSvgFileIdAndSharedToUserId(Long userSvgFileId, Long sharedToUserId);

    Optional<UserSvgFileV2Share> findByUserSvgFileIdAndSharedToUserIdAndStatus(Long userSvgFileId, Long sharedToUserId, String status);

    List<UserSvgFileV2Share> findByUserSvgFileIdAndStatus(Long userSvgFileId, String status);

    boolean existsByUserSvgFileIdAndSharedToUserIdAndStatus(Long userSvgFileId, Long sharedToUserId, String status);

    @Query("SELECT s.userSvgFile.id FROM UserSvgFileV2Share s WHERE s.sharedToUser.id = :userId AND s.status = :status")
    List<Long> findSharedFileIdsByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);
}

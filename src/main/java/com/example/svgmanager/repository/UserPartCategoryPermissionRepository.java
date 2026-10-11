package com.example.svgmanager.repository;

import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.UserPartCategoryPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface UserPartCategoryPermissionRepository extends JpaRepository<UserPartCategoryPermission, Long> {

    List<UserPartCategoryPermission> findByUserId(Long userId);

    List<UserPartCategoryPermission> findByUser(User user);

    @Query("SELECT p.partLibraryCategory.id FROM UserPartCategoryPermission p WHERE p.user.id = :userId")
    List<Long> findPartCategoryIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT p.partLibraryCategory.id FROM UserPartCategoryPermission p WHERE p.user.id IN :userIds")
    List<Long> findPartCategoryIdsByUserIds(@Param("userIds") Collection<Long> userIds);

    boolean existsByUserIdAndPartLibraryCategoryId(Long userId, Long partCategoryId);

    @Modifying
    @Query("DELETE FROM UserPartCategoryPermission p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserPartCategoryPermission p WHERE p.user.id = :userId AND p.partLibraryCategory.id NOT IN :categoryIds")
    void deleteByUserIdAndPartLibraryCategoryIdNotIn(@Param("userId") Long userId, @Param("categoryIds") Collection<Long> categoryIds);
}

package com.example.svgmanager.repository;

import com.example.svgmanager.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIsNullOrderByDisplayOrderAscIdAsc();

    List<Category> findByParentIdOrderByDisplayOrderAscIdAsc(Long parentId);

    List<Category> findByLevelOrderByDisplayOrderAscIdAsc(String level);

    Optional<Category> findByValueAndLevel(String value, String level);

    boolean existsByParentId(Long parentId);

    boolean existsByValueAndParentId(String value, Long parentId);

    boolean existsByValueAndParentIsNull(String value);

    @Query("SELECT c FROM Category c WHERE c.level = :level AND c.parent.value = :parentValue ORDER BY c.displayOrder ASC, c.id ASC")
    List<Category> findByLevelAndParentValue(@Param("level") String level, @Param("parentValue") String parentValue);
}

package com.example.svgmanager.repository;

import com.example.svgmanager.entity.UserSvgFile;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class UserSvgFileSpecification {

    private UserSvgFileSpecification() {}

    public static Specification<UserSvgFile> filter(
            String keyword,
            Long categoryId,
            Long vehicleNodeId,
            Long brandId,
            Long modelId,
            Long dealerId,
            Long userId,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            String status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Keyword search
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate fileNamePred = cb.like(cb.lower(root.get("fileName")), pattern);
                Predicate originalNamePred = cb.like(cb.lower(root.get("originalFileName")), pattern);
                Predicate descPred = cb.like(cb.lower(root.get("description")), pattern);
                Predicate brandPred = cb.like(cb.lower(root.get("brandName")), pattern);
                Predicate modelPred = cb.like(cb.lower(root.get("modelName")), pattern);
                Predicate userPred = cb.like(cb.lower(root.get("user").get("username")), pattern);
                Predicate fullNamePred = cb.like(cb.lower(root.get("user").get("fullName")), pattern);

                predicates.add(cb.or(fileNamePred, originalNamePred, descPred, brandPred, modelPred, userPred, fullNamePred));
            }

            // 2. Category
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // 3. Vehicle node / configuration
            if (vehicleNodeId != null) {
                predicates.add(cb.equal(root.get("vehicleNode").get("id"), vehicleNodeId));
            }

            // 4. Brand ID (exact node or ancestor)
            if (brandId != null) {
                Predicate directBrand = cb.equal(root.get("vehicleNode").get("id"), brandId);
                Predicate p1 = cb.equal(root.get("vehicleNode").get("parent").get("id"), brandId);
                Predicate p2 = cb.equal(root.get("vehicleNode").get("parent").get("parent").get("id"), brandId);
                Predicate p3 = cb.equal(root.get("vehicleNode").get("parent").get("parent").get("parent").get("id"), brandId);
                predicates.add(cb.or(directBrand, p1, p2, p3));
            }

            // 5. Model ID
            if (modelId != null) {
                Predicate directModel = cb.equal(root.get("vehicleNode").get("id"), modelId);
                Predicate p1 = cb.equal(root.get("vehicleNode").get("parent").get("id"), modelId);
                Predicate p2 = cb.equal(root.get("vehicleNode").get("parent").get("parent").get("id"), modelId);
                predicates.add(cb.or(directModel, p1, p2));
            }

            // 6. Dealer ID
            if (dealerId != null) {
                predicates.add(cb.equal(root.get("dealer").get("id"), dealerId));
            }

            // 7. User ID
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            // 8. Created From
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }

            // 9. Created To
            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }

            // 10. Status
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status.trim().toUpperCase()));
            } else {
                // Default: exclude DELETED
                predicates.add(cb.notEqual(root.get("status"), "DELETED"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

package com.example.svgmanager.repository.internal.v2;

import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.v2.UserSvgFileV2;
import com.example.svgmanager.entity.v2.UserSvgFileV2Share;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserSvgFileV2Specification {

    public static Specification<UserSvgFileV2> filterAccessibleByUser(
            User user,
            String keyword,
            Long categoryId,
            String status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. User access control: Owner OR shared in user_svg_file_v2_share
            if (user != null) {
                Predicate isOwner = cb.equal(root.get("user").get("id"), user.getId());

                Subquery<Long> shareSubquery = query.subquery(Long.class);
                Root<UserSvgFileV2Share> shareRoot = shareSubquery.from(UserSvgFileV2Share.class);
                shareSubquery.select(shareRoot.get("userSvgFile").get("id"))
                        .where(
                                cb.equal(shareRoot.get("sharedToUser").get("id"), user.getId()),
                                cb.equal(cb.upper(shareRoot.get("status")), "ACTIVE")
                        );

                Predicate isShared = root.get("id").in(shareSubquery);
                predicates.add(cb.or(isOwner, isShared));
            }

            // 2. Status filter
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            } else {
                predicates.add(cb.notEqual(cb.upper(root.get("status")), "DELETED"));
            }

            // 3. Category filter
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // 4. Keyword search
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate fileNameLike = cb.like(cb.lower(root.get("fileName")), pattern);
                Predicate brandLike = cb.like(cb.lower(root.get("brandName")), pattern);
                Predicate modelLike = cb.like(cb.lower(root.get("modelName")), pattern);
                predicates.add(cb.or(fileNameLike, brandLike, modelLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<UserSvgFileV2> filterForAdmin(
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
                Predicate fileNameLike = cb.like(cb.lower(root.get("fileName")), pattern);
                Predicate origNameLike = cb.like(cb.lower(root.get("originalFileName")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                Predicate brandLike = cb.like(cb.lower(root.get("brandName")), pattern);
                Predicate modelLike = cb.like(cb.lower(root.get("modelName")), pattern);
                Predicate userLike = cb.like(cb.lower(root.get("user").get("username")), pattern);
                Predicate fullNameLike = cb.like(cb.lower(root.get("user").get("fullName")), pattern);
                predicates.add(cb.or(fileNameLike, origNameLike, descLike, brandLike, modelLike, userLike, fullNameLike));
            }

            // 2. Category
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // 3. Vehicle node
            if (vehicleNodeId != null) {
                predicates.add(cb.equal(root.get("vehicleNode").get("id"), vehicleNodeId));
            }

            // 4. Brand ID
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
                String trimmedStatus = status.trim();
                if (!"ALL".equalsIgnoreCase(trimmedStatus)) {
                    predicates.add(cb.equal(root.get("status"), trimmedStatus.toUpperCase()));
                }
            } else {
                predicates.add(cb.notEqual(root.get("status"), "DELETED"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

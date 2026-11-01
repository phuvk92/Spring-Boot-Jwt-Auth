package com.example.svgmanager.repository.internal.v2;

import com.example.svgmanager.entity.v2.UnifiedShare;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class UnifiedShareSpecification {

    private UnifiedShareSpecification() {
    }

    public static Specification<UnifiedShare> filter(
            String shareType,
            String status,
            String keyword,
            Long dealerId,
            Long fileId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(shareType) && !"ALL".equalsIgnoreCase(shareType.trim())) {
                predicates.add(cb.equal(cb.upper(root.get("shareType")), shareType.trim().toUpperCase()));
            }

            if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status.trim())) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            if (dealerId != null) {
                Predicate byDealer = cb.equal(root.get("sharedByDealerId"), dealerId);
                Predicate toDealer = cb.equal(root.get("sharedToDealerId"), dealerId);
                predicates.add(cb.or(byDealer, toDealer));
            }

            if (fileId != null) {
                predicates.add(cb.equal(root.get("fileId"), fileId));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate shareIdPred = cb.like(cb.lower(root.get("shareId")), pattern);
                Predicate fileNamePred = cb.like(cb.lower(root.get("fileName")), pattern);
                Predicate sharedByPred = cb.like(cb.lower(root.get("sharedByUsername")), pattern);
                Predicate sharedToPred = cb.like(cb.lower(root.get("sharedToUsername")), pattern);
                Predicate clientPred = cb.like(cb.lower(root.get("ownerClientId")), pattern);
                predicates.add(cb.or(shareIdPred, fileNamePred, sharedByPred, sharedToPred, clientPred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

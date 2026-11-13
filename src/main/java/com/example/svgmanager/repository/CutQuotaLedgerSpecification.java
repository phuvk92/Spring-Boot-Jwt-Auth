package com.example.svgmanager.repository;

import com.example.svgmanager.entity.CutQuotaLedger;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class CutQuotaLedgerSpecification {

    private CutQuotaLedgerSpecification() {
    }

    public static Specification<CutQuotaLedger> filter(
            Long dealerId,
            Long userId,
            String transactionType,
            String status,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (dealerId != null) {
                predicates.add(cb.equal(root.get("dealer").get("id"), dealerId));
            }

            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            if (StringUtils.hasText(transactionType)) {
                predicates.add(cb.equal(cb.upper(root.get("transactionType")), transactionType.trim().toUpperCase()));
            }

            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

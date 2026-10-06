package com.example.svgmanager.repository;

import com.example.svgmanager.entity.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    public static Specification<AuditLog> filter(
            String keyword,
            String entity,
            String actor,
            String actorRole,
            String action,
            LocalDateTime fromTimestamp,
            LocalDateTime toTimestamp
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate actorPred = cb.like(cb.lower(root.get("actor")), pattern);
                Predicate actionPred = cb.like(cb.lower(root.get("action")), pattern);
                Predicate entityPred = cb.like(cb.lower(root.get("entity")), pattern);
                Predicate detailsPred = cb.like(cb.lower(root.get("details")), pattern);
                predicates.add(cb.or(actorPred, actionPred, entityPred, detailsPred));
            }

            if (StringUtils.hasText(entity)) {
                predicates.add(cb.equal(cb.lower(root.get("entity")), entity.trim().toLowerCase()));
            }

            if (StringUtils.hasText(actor)) {
                predicates.add(cb.like(cb.lower(root.get("actor")), "%" + actor.trim().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(actorRole)) {
                predicates.add(cb.equal(cb.upper(root.get("actorRole")), actorRole.trim().toUpperCase()));
            }

            if (StringUtils.hasText(action)) {
                predicates.add(cb.like(cb.lower(root.get("action")), "%" + action.trim().toLowerCase() + "%"));
            }

            if (fromTimestamp != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), fromTimestamp));
            }

            if (toTimestamp != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), toTimestamp));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

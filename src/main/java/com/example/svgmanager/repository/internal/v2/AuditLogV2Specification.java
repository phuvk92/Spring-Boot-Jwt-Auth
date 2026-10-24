package com.example.svgmanager.repository.internal.v2;

import com.example.svgmanager.entity.v2.AuditLogV2;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AuditLogV2Specification {

    private AuditLogV2Specification() {
    }

    public static Specification<AuditLogV2> filter(
            String keyword,
            String clientId,
            String clientName,
            String username,
            Long userId,
            String role,
            Long dealerId,
            String endpoint,
            String httpMethod,
            Integer httpStatus,
            String result,
            String requestId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String term = "%" + keyword.trim().toLowerCase() + "%";
                Predicate reqId = cb.like(cb.lower(root.get("requestId")), term);
                Predicate ep = cb.like(cb.lower(root.get("endpoint")), term);
                Predicate user = cb.like(cb.lower(root.get("username")), term);
                Predicate cId = cb.like(cb.lower(root.get("clientId")), term);
                Predicate cName = cb.like(cb.lower(root.get("clientName")), term);
                Predicate err = cb.like(cb.lower(root.get("errorCode")), term);
                predicates.add(cb.or(reqId, ep, user, cId, cName, err));
            }

            if (StringUtils.hasText(clientId)) {
                predicates.add(cb.equal(cb.lower(root.get("clientId")), clientId.trim().toLowerCase()));
            }

            if (StringUtils.hasText(clientName)) {
                predicates.add(cb.like(cb.lower(root.get("clientName")), "%" + clientName.trim().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(username)) {
                predicates.add(cb.equal(cb.lower(root.get("username")), username.trim().toLowerCase()));
            }

            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }

            if (StringUtils.hasText(role)) {
                predicates.add(cb.equal(root.get("role"), role.trim().toUpperCase()));
            }

            if (dealerId != null) {
                predicates.add(cb.equal(root.get("dealerId"), dealerId));
            }

            if (StringUtils.hasText(endpoint)) {
                predicates.add(cb.like(cb.lower(root.get("endpoint")), "%" + endpoint.trim().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(httpMethod)) {
                predicates.add(cb.equal(cb.upper(root.get("httpMethod")), httpMethod.trim().toUpperCase()));
            }

            if (httpStatus != null) {
                predicates.add(cb.equal(root.get("httpStatus"), httpStatus));
            }

            if (StringUtils.hasText(result)) {
                predicates.add(cb.equal(cb.upper(root.get("result")), result.trim().toUpperCase()));
            }

            if (StringUtils.hasText(requestId)) {
                predicates.add(cb.equal(root.get("requestId"), requestId.trim()));
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

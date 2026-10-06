package com.example.svgmanager.repository.internal.v2;

import com.example.svgmanager.entity.User;
import com.example.svgmanager.entity.v2.UserSvgFileV2;
import com.example.svgmanager.entity.v2.UserSvgFileV2Share;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

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
}

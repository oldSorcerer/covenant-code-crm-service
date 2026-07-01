package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class TeacherSpecifications {

    private TeacherSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<User> hasRole(RoleName role) {
        return (root, query, cb) -> {
            if (role == null) return cb.conjunction();
            return cb.equal(root.get("role").get("name"), role);
        };
    }

    public static Specification<User> searchByText(String text) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(text)) {
                return cb.conjunction();
            }

            String searchLower = "%" + text.trim().toLowerCase() + "%";

            Predicate firstName = cb.like(
                    cb.lower(root.get("firstName")),
                    searchLower
            );

            Predicate lastName = cb.like(
                    cb.lower(root.get("lastName")),
                    searchLower
            );

            Predicate email = cb.like(
                    cb.lower(root.get("email")),
                    searchLower
            );


            return cb.or(firstName, lastName, email);
        };
    }
}

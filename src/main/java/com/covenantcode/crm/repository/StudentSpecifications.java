package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.Student;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class StudentSpecifications {

    private StudentSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<Student> searchByText(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) {
                return cb.conjunction();
            }

            String searchLower = "%" + search.toLowerCase() + "%";

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

            Predicate phone = cb.like(
                    cb.lower(root.get("phone")),
                    searchLower
            );

            return cb.or(firstName, lastName, email, phone);
        };
    }
}

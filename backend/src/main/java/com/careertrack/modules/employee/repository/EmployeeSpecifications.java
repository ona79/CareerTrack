package com.careertrack.modules.employee.repository;

import com.careertrack.modules.employee.entity.Employee;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    /** Charge département et poste dans la même requête (évite les requêtes en cascade). */
    public static Specification<Employee> withDetails() {
        return (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType())) {
                root.fetch("department", JoinType.LEFT);
                root.fetch("position", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    public static Specification<Employee> inDepartment(UUID departmentId) {
        return (root, query, cb) -> departmentId == null
                ? cb.conjunction()
                : cb.equal(root.get("department").get("id"), departmentId);
    }

    public static Specification<Employee> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            String escaped = search.trim().toLowerCase()
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            String like = "%" + escaped + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), like, '\\'),
                    cb.like(cb.lower(root.get("lastName")), like, '\\'),
                    cb.like(cb.lower(root.get("employeeNumber")), like, '\\'),
                    cb.like(cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))), like, '\\')
            );
        };
    }
}
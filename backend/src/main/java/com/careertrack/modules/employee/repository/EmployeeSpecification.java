package com.careertrack.modules.employee.repository;

import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.EmployeeStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EmployeeSpecification {

    public static Specification<Employee> withFilters(
            String search,
            UUID departmentId,
            EmployeeStatus status,
            UUID managerEmployeeId
    ) {
        return (root, query, cb) -> {
            // Eager fetch department and position for data queries (avoid count queries)
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("department", JoinType.LEFT);
                root.fetch("position", JoinType.LEFT);
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            // 1. Search (prénom, nom, matricule)
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), pattern);
                Predicate empNumberMatch = cb.like(cb.lower(root.get("employeeNumber")), pattern);
                predicates.add(cb.or(firstNameMatch, lastNameMatch, empNumberMatch));
            }

            // 2. Department filter
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }

            // 3. Status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                // Par défaut, exclure les employés TERMINATED
                predicates.add(cb.notEqual(root.get("status"), EmployeeStatus.TERMINATED));
            }

            // 4. Scoping Manager (ses subordonnés directs + lui-même)
            if (managerEmployeeId != null) {
                Predicate isSubordinate = cb.equal(root.get("manager").get("id"), managerEmployeeId);
                Predicate isSelf = cb.equal(root.get("id"), managerEmployeeId);
                predicates.add(cb.or(isSubordinate, isSelf));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

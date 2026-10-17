package com.careertrack.modules.employee.repository;

import com.careertrack.modules.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByEmployeeNumber(String employeeNumber);
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByUserId(UUID userId);

    List<Employee> findByDepartmentId(UUID departmentId);
    List<Employee> findByManagerId(UUID managerId);
    List<Employee> findByManagerIsNull();

    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.department LEFT JOIN FETCH e.position WHERE e.manager IS NULL")
    List<Employee> findRootEmployeesWithDetails();

    Page<Employee> findByDepartmentId(UUID departmentId, Pageable pageable);
}

package com.careertrack.modules.employee.repository;

import com.careertrack.modules.employee.entity.PositionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, UUID> {

    @Query("SELECT ph FROM PositionHistory ph " +
           "LEFT JOIN FETCH ph.previousPosition " +
           "LEFT JOIN FETCH ph.newPosition " +
           "LEFT JOIN FETCH ph.previousDepartment " +
           "LEFT JOIN FETCH ph.newDepartment " +
           "WHERE ph.employee.id = :employeeId " +
           "ORDER BY ph.effectiveDate DESC, ph.createdAt DESC")
    List<PositionHistory> findByEmployeeIdOrderByEffectiveDateDesc(@Param("employeeId") UUID employeeId);
}

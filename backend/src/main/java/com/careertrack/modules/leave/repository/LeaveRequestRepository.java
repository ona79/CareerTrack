package com.careertrack.modules.leave.repository;

import com.careertrack.modules.leave.entity.LeaveRequest;
import com.careertrack.modules.leave.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);
    List<LeaveRequest> findByEmployeeManagerIdAndStatus(UUID managerId, LeaveStatus status);

    @Query("SELECT lr FROM LeaveRequest lr " +
           "WHERE lr.employee.department.id = :departmentId " +
           "AND lr.status IN ('SUBMITTED', 'APPROVED_N1', 'APPROVED_DRH') " +
           "AND lr.startDate <= :endDate AND lr.endDate >= :startDate")
    List<LeaveRequest> findTeamLeavesInRange(
            @Param("departmentId") UUID departmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

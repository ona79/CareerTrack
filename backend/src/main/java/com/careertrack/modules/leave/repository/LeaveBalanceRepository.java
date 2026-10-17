package com.careertrack.modules.leave.repository;

import com.careertrack.modules.leave.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {
    List<LeaveBalance> findByEmployeeIdAndPeriodYear(UUID employeeId, int periodYear);
    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndPeriodYear(UUID employeeId, UUID leaveTypeId, int periodYear);
}

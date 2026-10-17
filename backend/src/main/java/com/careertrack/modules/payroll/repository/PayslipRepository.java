package com.careertrack.modules.payroll.repository;

import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.entity.PayslipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, UUID> {
    List<Payslip> findByPeriodYearAndPeriodMonth(int periodYear, int periodMonth);
    List<Payslip> findByEmployeeIdOrderByPeriodYearDescPeriodMonthDesc(UUID employeeId);
    Optional<Payslip> findByEmployeeIdAndPeriodYearAndPeriodMonth(UUID employeeId, int periodYear, int periodMonth);
    List<Payslip> findByStatus(PayslipStatus status);
}

package com.careertrack.modules.payroll.repository;

import com.careertrack.modules.payroll.entity.PayslipLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayslipLineRepository extends JpaRepository<PayslipLine, UUID> {
    List<PayslipLine> findByPayslipId(UUID payslipId);
}

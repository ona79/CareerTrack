package com.careertrack.modules.payroll.service;

import com.careertrack.modules.payroll.dto.PayslipResponseDto;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.entity.PayslipStatus;

import java.util.List;
import java.util.UUID;

public interface PayrollService {
    List<PayslipResponseDto> getPayslipsByPeriod(String period);
    List<PayslipResponseDto> getPayslipsByEmployee(UUID employeeId);
    PayslipResponseDto getPayslipById(UUID payslipId);
    Payslip generateMonthlyPayslip(UUID employeeId, String period);
    List<PayslipResponseDto> generatePayrollForPeriod(String period);
    PayslipResponseDto updateStatus(UUID payslipId, PayslipStatus newStatus);
}

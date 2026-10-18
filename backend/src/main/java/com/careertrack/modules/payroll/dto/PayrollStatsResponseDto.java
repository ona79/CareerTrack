package com.careertrack.modules.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollStatsResponseDto {
    private String period;
    private int totalEmployeesPaid;
    private BigDecimal totalGrossSalary;
    private BigDecimal totalNetSalary;
    private BigDecimal totalDeductions;
    private BigDecimal totalEmployerContributions;
    private BigDecimal totalEmployerCost;
    private List<DepartmentPayrollStatsDto> departmentBreakdown;
}

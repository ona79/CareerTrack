package com.careertrack.modules.dashboard.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollKpiDto {
    private BigDecimal totalGrossSalary;
    private BigDecimal totalNetSalary;
    private BigDecimal totalEmployerContributions;
    private long payslipsCount;
    private List<MonthlyPayrollTrendDto> monthlyTrend;
}

package com.careertrack.modules.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentPayrollStatsDto {
    private String departmentName;
    private int employeeCount;
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
    private BigDecimal employerCost;
}

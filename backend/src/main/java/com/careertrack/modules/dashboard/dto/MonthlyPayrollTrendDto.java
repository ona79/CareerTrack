package com.careertrack.modules.dashboard.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyPayrollTrendDto {
    private int month;
    private String monthLabel;
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
    private BigDecimal employerContributions;
}

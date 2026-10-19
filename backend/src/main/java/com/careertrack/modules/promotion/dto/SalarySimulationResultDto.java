package com.careertrack.modules.promotion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalarySimulationResultDto {
    private UUID id;
    private String title;
    private UUID departmentId;
    private String departmentName;
    private BigDecimal averageIncreasePercentage;
    private BigDecimal currentMonthlyPayroll;
    private BigDecimal projectedMonthlyPayroll;
    private BigDecimal monthlyDifference;
    private BigDecimal projectedAnnualPayrollImpact;
    private int affectedEmployeesCount;
    private Instant simulatedAt;
    private String createdByName;
}

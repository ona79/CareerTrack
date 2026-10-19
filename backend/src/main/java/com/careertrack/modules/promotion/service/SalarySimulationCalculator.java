package com.careertrack.modules.promotion.service;

import com.careertrack.modules.employee.entity.Contract;
import com.careertrack.modules.promotion.dto.SalarySimulationResultDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class SalarySimulationCalculator {

    public SalarySimulationResultDto calculateSimulation(
            String title,
            UUID departmentId,
            String departmentName,
            BigDecimal increasePercentage,
            List<Contract> activeContracts,
            String createdByName
    ) {
        BigDecimal safePercentage = increasePercentage != null ? increasePercentage : BigDecimal.ZERO;
        if (safePercentage.compareTo(BigDecimal.ZERO) < 0) {
            safePercentage = BigDecimal.ZERO;
        }

        BigDecimal currentMonthly = BigDecimal.ZERO;
        int count = 0;

        if (activeContracts != null) {
            for (Contract contract : activeContracts) {
                if (contract.getGrossSalary() != null) {
                    currentMonthly = currentMonthly.add(contract.getGrossSalary());
                }
                count++;
            }
        }

        BigDecimal monthlyDiff = currentMonthly.multiply(safePercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal projectedMonthly = currentMonthly.add(monthlyDiff);
        BigDecimal annualImpact = monthlyDiff.multiply(BigDecimal.valueOf(12));

        return SalarySimulationResultDto.builder()
                .title(title)
                .departmentId(departmentId)
                .departmentName(departmentName)
                .averageIncreasePercentage(safePercentage.setScale(2, RoundingMode.HALF_UP))
                .currentMonthlyPayroll(currentMonthly.setScale(2, RoundingMode.HALF_UP))
                .projectedMonthlyPayroll(projectedMonthly.setScale(2, RoundingMode.HALF_UP))
                .monthlyDifference(monthlyDiff.setScale(2, RoundingMode.HALF_UP))
                .projectedAnnualPayrollImpact(annualImpact.setScale(2, RoundingMode.HALF_UP))
                .affectedEmployeesCount(count)
                .simulatedAt(Instant.now())
                .createdByName(createdByName)
                .build();
    }
}

package com.careertrack.modules.promotion.service;

import com.careertrack.modules.employee.entity.Contract;
import com.careertrack.modules.promotion.dto.SalarySimulationResultDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SalarySimulationCalculatorTest {

    private final SalarySimulationCalculator calculator = new SalarySimulationCalculator();

    @Test
    @DisplayName("Calcul de la simulation de masse salariale avec 5% d'augmentation")
    void shouldCalculateSimulationWithFivePercentIncrease() {
        Contract c1 = Contract.builder().id(UUID.randomUUID()).grossSalary(new BigDecimal("500000")).build();
        Contract c2 = Contract.builder().id(UUID.randomUUID()).grossSalary(new BigDecimal("1500000")).build();

        SalarySimulationResultDto result = calculator.calculateSimulation(
                "Plan Revalorisation 5%",
                null,
                "Global",
                new BigDecimal("5.0"),
                List.of(c1, c2),
                "DRH Test"
        );

        // Masse mensuelle = 500 000 + 1 500 000 = 2 000 000 FCFA
        assertThat(result.getCurrentMonthlyPayroll()).isEqualByComparingTo("2000000.00");
        // Différence mensuelle = 2 000 000 * 5% = 100 000 FCFA
        assertThat(result.getMonthlyDifference()).isEqualByComparingTo("100000.00");
        // Masse mensuelle projetée = 2 100 000 FCFA
        assertThat(result.getProjectedMonthlyPayroll()).isEqualByComparingTo("2100000.00");
        // Impact annuel = 100 000 * 12 = 1 200 000 FCFA
        assertThat(result.getProjectedAnnualPayrollImpact()).isEqualByComparingTo("1200000.00");
        assertThat(result.getAffectedEmployeesCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Calcul de la simulation avec liste vide de contrats")
    void shouldHandleEmptyContracts() {
        SalarySimulationResultDto result = calculator.calculateSimulation(
                "Plan Vide",
                null,
                "Global",
                new BigDecimal("10.0"),
                List.of(),
                "DRH Test"
        );

        assertThat(result.getCurrentMonthlyPayroll()).isEqualByComparingTo("0.00");
        assertThat(result.getMonthlyDifference()).isEqualByComparingTo("0.00");
        assertThat(result.getProjectedMonthlyPayroll()).isEqualByComparingTo("0.00");
        assertThat(result.getProjectedAnnualPayrollImpact()).isEqualByComparingTo("0.00");
        assertThat(result.getAffectedEmployeesCount()).isEqualTo(0);
    }
}

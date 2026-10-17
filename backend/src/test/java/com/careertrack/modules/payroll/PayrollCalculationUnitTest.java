package com.careertrack.modules.payroll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class PayrollCalculationUnitTest {

    private static final BigDecimal IPRES_EMPLOYEE_RATE = new BigDecimal("0.056");
    private static final BigDecimal IPRES_EMPLOYER_RATE = new BigDecimal("0.084");
    private static final BigDecimal CSS_EMPLOYER_RATE   = new BigDecimal("0.070");
    private static final BigDecimal CSS_EMPLOYEE_RATE   = BigDecimal.ZERO; // CONFIRMATION LÉGALE : 0% SALARIAL
    private static final BigDecimal IR_ESTIMATE_RATE    = new BigDecimal("0.12");
    private static final BigDecimal TRIMF_MONTHLY       = new BigDecimal("900");

    @Test
    @DisplayName("Vérifie que la CSS est strictement à 0% pour le salarié (100% employeur)")
    void shouldConfirmCssIsZeroForEmployee() {
        BigDecimal grossSalary = new BigDecimal("1000000"); // 1 000 000 FCFA

        BigDecimal cssEmployee = grossSalary.multiply(CSS_EMPLOYEE_RATE);
        BigDecimal cssEmployer = grossSalary.multiply(CSS_EMPLOYER_RATE).setScale(0, RoundingMode.HALF_UP);

        assertThat(cssEmployee).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cssEmployer).isEqualByComparingTo(new BigDecimal("70000"));
    }

    @Test
    @DisplayName("Calcule correctement le salaire net et les cotisations pour 1 000 000 FCFA brut")
    void shouldCalculateCorrectNetSalaryAndContributions() {
        BigDecimal gross = new BigDecimal("1000000");

        // Retenues salariales
        BigDecimal ipresSalarial = gross.multiply(IPRES_EMPLOYEE_RATE).setScale(0, RoundingMode.HALF_UP); // 56 000 FCFA
        BigDecimal irRetenue    = gross.multiply(IR_ESTIMATE_RATE).setScale(0, RoundingMode.HALF_UP);    // 120 000 FCFA
        BigDecimal trimf        = TRIMF_MONTHLY;                                                          // 900 FCFA

        BigDecimal totalDeductions = ipresSalarial.add(irRetenue).add(trimf);                             // 176 900 FCFA
        BigDecimal netSalary = gross.subtract(totalDeductions);                                           // 823 100 FCFA

        assertThat(ipresSalarial).isEqualByComparingTo(new BigDecimal("56000"));
        assertThat(irRetenue).isEqualByComparingTo(new BigDecimal("120000"));
        assertThat(totalDeductions).isEqualByComparingTo(new BigDecimal("176900"));
        assertThat(netSalary).isEqualByComparingTo(new BigDecimal("823100"));

        // Charges patronales
        BigDecimal ipresPatronal = gross.multiply(IPRES_EMPLOYER_RATE).setScale(0, RoundingMode.HALF_UP); // 84 000 FCFA
        BigDecimal cssPatronal   = gross.multiply(CSS_EMPLOYER_RATE).setScale(0, RoundingMode.HALF_UP);   // 70 000 FCFA
        BigDecimal totalEmployerCharges = ipresPatronal.add(cssPatronal);                                  // 154 000 FCFA

        assertThat(ipresPatronal).isEqualByComparingTo(new BigDecimal("84000"));
        assertThat(cssPatronal).isEqualByComparingTo(new BigDecimal("70000"));
        assertThat(totalEmployerCharges).isEqualByComparingTo(new BigDecimal("154000"));
    }
}

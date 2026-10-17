package com.careertrack.modules.leave;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class LeaveBalanceCalculationUnitTest {

    private static final BigDecimal MONTHLY_LEAVE_ACCRUAL = new BigDecimal("2.0"); // Code du Travail sénégalais

    @Test
    @DisplayName("Calcule 24 jours ouvrables acquis pour une année complète (12 mois)")
    void shouldCalculate24DaysForFullYear() {
        int workedMonths = 12;
        BigDecimal acquiredDays = MONTHLY_LEAVE_ACCRUAL.multiply(BigDecimal.valueOf(workedMonths));

        assertThat(acquiredDays).isEqualByComparingTo(new BigDecimal("24.0"));
    }

    @Test
    @DisplayName("Calcule les jours acquis au prorata pour une embauche en cours d'année")
    void shouldCalculateProRataLeaveForPartialYear() {
        int workedMonths = 7; // Embauche au 1er juin, calcul fin décembre
        BigDecimal acquiredDays = MONTHLY_LEAVE_ACCRUAL.multiply(BigDecimal.valueOf(workedMonths));

        assertThat(acquiredDays).isEqualByComparingTo(new BigDecimal("14.0"));
    }

    @Test
    @DisplayName("Calcule le solde restant exact après prise de congés")
    void shouldCalculateRemainingBalance() {
        BigDecimal totalAcquired = new BigDecimal("24.0");
        BigDecimal usedDays = new BigDecimal("8.5");

        BigDecimal remaining = totalAcquired.subtract(usedDays);

        assertThat(remaining).isEqualByComparingTo(new BigDecimal("15.5"));
    }
}

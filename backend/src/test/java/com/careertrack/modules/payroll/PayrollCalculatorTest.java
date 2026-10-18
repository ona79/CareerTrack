package com.careertrack.modules.payroll;

import com.careertrack.modules.payroll.entity.PayrollTaxBracket;
import com.careertrack.modules.payroll.entity.PayrollTrimfBracket;
import com.careertrack.modules.payroll.entity.PayslipLineCategory;
import com.careertrack.modules.payroll.entity.PayslipLineType;
import com.careertrack.modules.payroll.service.PayrollCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayrollCalculatorTest {

    private PayrollCalculator calculator;
    private Map<String, String> defaultParams;
    private List<PayrollTaxBracket> defaultTaxBrackets;
    private List<PayrollTrimfBracket> defaultTrimfBrackets;

    @BeforeEach
    void setUp() {
        calculator = new PayrollCalculator();

        defaultParams = new HashMap<>();
        defaultParams.put("IPRES_RG_EMPLOYEE_RATE", "0.056");
        defaultParams.put("IPRES_RG_EMPLOYER_RATE", "0.084");
        defaultParams.put("IPRES_RG_CEILING", "432000");
        defaultParams.put("IPRES_RC_EMPLOYEE_RATE", "0.024");
        defaultParams.put("IPRES_RC_EMPLOYER_RATE", "0.036");
        defaultParams.put("IPRES_RC_CEILING", "1296000");
        defaultParams.put("PAYROLL_IPRES_RC_CADRES_ONLY", "true");
        defaultParams.put("CSS_PF_EMPLOYER_RATE", "0.070");
        defaultParams.put("CSS_AT_EMPLOYER_RATE", "0.010");
        defaultParams.put("CSS_CEILING", "63000");
        defaultParams.put("CFCE_EMPLOYER_RATE", "0.030");
        defaultParams.put("IPM_EMPLOYEE_RATE", "0.030");
        defaultParams.put("IPM_EMPLOYER_RATE", "0.030");
        defaultParams.put("IPM_CEILING", "250000");
        defaultParams.put("PAYROLL_IPM_DEDUCTIBLE_FROM_IR", "true");
        defaultParams.put("FRAIS_PRO_RATE", "0.30");
        defaultParams.put("FRAIS_PRO_MONTHLY_CEILING", "75000");
        defaultParams.put("TAX_PARTS_SINGLE", "1.0");
        defaultParams.put("TAX_PARTS_MARRIED", "1.5");
        defaultParams.put("TAX_PARTS_PER_DEPENDENT", "0.5");
        defaultParams.put("TAX_PARTS_MAX", "5.0");

        defaultTaxBrackets = List.of(
                PayrollTaxBracket.builder().bracketOrder(1).minAnnualIncome(new BigDecimal("0")).maxAnnualIncome(new BigDecimal("630000")).rate(new BigDecimal("0.0000")).validFrom(LocalDate.of(2020, 1, 1)).build(),
                PayrollTaxBracket.builder().bracketOrder(2).minAnnualIncome(new BigDecimal("630000")).maxAnnualIncome(new BigDecimal("1500000")).rate(new BigDecimal("0.2000")).validFrom(LocalDate.of(2020, 1, 1)).build(),
                PayrollTaxBracket.builder().bracketOrder(3).minAnnualIncome(new BigDecimal("1500000")).maxAnnualIncome(new BigDecimal("4000000")).rate(new BigDecimal("0.3000")).validFrom(LocalDate.of(2020, 1, 1)).build(),
                PayrollTaxBracket.builder().bracketOrder(4).minAnnualIncome(new BigDecimal("4000000")).maxAnnualIncome(new BigDecimal("8000000")).rate(new BigDecimal("0.3500")).validFrom(LocalDate.of(2020, 1, 1)).build(),
                PayrollTaxBracket.builder().bracketOrder(5).minAnnualIncome(new BigDecimal("8000000")).maxAnnualIncome(new BigDecimal("13500000")).rate(new BigDecimal("0.3700")).validFrom(LocalDate.of(2020, 1, 1)).build(),
                PayrollTaxBracket.builder().bracketOrder(6).minAnnualIncome(new BigDecimal("13500000")).maxAnnualIncome(null).rate(new BigDecimal("0.4000")).validFrom(LocalDate.of(2020, 1, 1)).build()
        );

        defaultTrimfBrackets = List.of(
                PayrollTrimfBracket.builder().bracketOrder(1).minAnnualGross(new BigDecimal("0")).maxAnnualGross(new BigDecimal("600000")).annualAmount(new BigDecimal("900")).validFrom(LocalDate.of(2020, 1, 1)).build(),
                PayrollTrimfBracket.builder().bracketOrder(2).minAnnualGross(new BigDecimal("600000")).maxAnnualGross(new BigDecimal("1200000")).annualAmount(new BigDecimal("3600")).validFrom(LocalDate.of(2020, 1, 1)).build(),
                PayrollTrimfBracket.builder().bracketOrder(3).minAnnualGross(new BigDecimal("1200000")).maxAnnualGross(new BigDecimal("2400000")).annualAmount(new BigDecimal("4800")).validFrom(LocalDate.of(2020, 1, 1)).build(),
                PayrollTrimfBracket.builder().bracketOrder(4).minAnnualGross(new BigDecimal("2400000")).maxAnnualGross(new BigDecimal("3600000")).annualAmount(new BigDecimal("12000")).validFrom(LocalDate.of(2020, 1, 1)).build(),
                PayrollTrimfBracket.builder().bracketOrder(5).minAnnualGross(new BigDecimal("3600000")).maxAnnualGross(null).annualAmount(new BigDecimal("36000")).validFrom(LocalDate.of(2020, 1, 1)).build()
        );
    }

    @Test
    @DisplayName("Cas témoin salarié cadre à 950 000 FCFA (Célibataire, 1 part, hors TRIMF)")
    void shouldCalculateCadreWitnessCaseEmployee() {
        // Calcul sans TRIMF pour vérifier exactement les attendus témoins indépendants
        PayrollCalculator.CalculationResult result = calculator.calculate(
                new BigDecimal("950000"),
                true, // isCadre
                "CELIBATAIRE",
                0, // 0 enfant
                defaultParams,
                defaultTaxBrackets,
                Collections.emptyList() // hors TRIMF
        );

        assertThat(result.grossSalary()).isEqualByComparingTo(new BigDecimal("950000"));
        assertThat(result.taxParts()).isEqualByComparingTo(new BigDecimal("1.0"));

        // IPRES RG salarié : 432 000 * 5.6% = 24 192
        BigDecimal ipresRg = findLineAmount(result, PayslipLineType.IPRES_RG_EMPLOYEE);
        assertThat(ipresRg).isEqualByComparingTo(new BigDecimal("24192"));

        // IPRES RC salarié : (950 000 - 432 000) * 2.4% = 518 000 * 2.4% = 12 432
        BigDecimal ipresRc = findLineAmount(result, PayslipLineType.IPRES_RC_EMPLOYEE);
        assertThat(ipresRc).isEqualByComparingTo(new BigDecimal("12432"));

        // IPM salarié : 250 000 * 3% = 7 500
        BigDecimal ipm = findLineAmount(result, PayslipLineType.IPM);
        assertThat(ipm).isEqualByComparingTo(new BigDecimal("7500"));

        // Brut imposable = 950 000 - (24 192 + 12 432 + 7 500) = 905 876
        assertThat(result.taxableGross()).isEqualByComparingTo(new BigDecimal("905876"));

        // Net imposable mensuel = 905 876 - 75 000 (plafond frais pro) = 830 876
        assertThat(result.netTaxable()).isEqualByComparingTo(new BigDecimal("830876"));

        // IR mensuel calculé sur barème annuel = 254 424 FCFA
        BigDecimal ir = findLineAmount(result, PayslipLineType.TAX_IR);
        assertThat(ir).isEqualByComparingTo(new BigDecimal("254424"));

        // Total retenues = 24 192 + 12 432 + 7 500 + 254 424 = 298 548
        assertThat(result.totalDeductions()).isEqualByComparingTo(new BigDecimal("298548"));

        // Net à payer = 950 000 - 298 548 = 651 452 FCFA
        assertThat(result.netSalary()).isEqualByComparingTo(new BigDecimal("651452"));
    }

    @Test
    @DisplayName("Cas témoin patronal indépendant à 950 000 FCFA (Cadre)")
    void shouldCalculateCadreWitnessCaseEmployer() {
        PayrollCalculator.CalculationResult result = calculator.calculate(
                new BigDecimal("950000"),
                true,
                "CELIBATAIRE",
                0,
                defaultParams,
                defaultTaxBrackets,
                Collections.emptyList()
        );

        // IPRES RG patronal : 432 000 * 8.4% = 36 288
        BigDecimal ipresRgPatronal = findLineAmount(result, PayslipLineType.IPRES_RG_EMPLOYER);
        assertThat(ipresRgPatronal).isEqualByComparingTo(new BigDecimal("36288"));

        // IPRES RC patronal : 518 000 * 3.6% = 18 648
        BigDecimal ipresRcPatronal = findLineAmount(result, PayslipLineType.IPRES_RC_EMPLOYER);
        assertThat(ipresRcPatronal).isEqualByComparingTo(new BigDecimal("18648"));

        // CSS PF patronal : 63 000 * 7% = 4 410
        BigDecimal cssPf = findLineAmount(result, PayslipLineType.CSS_PF);
        assertThat(cssPf).isEqualByComparingTo(new BigDecimal("4410"));

        // CSS AT patronal : 63 000 * 1% = 630
        BigDecimal cssAt = findLineAmount(result, PayslipLineType.CSS_AT);
        assertThat(cssAt).isEqualByComparingTo(new BigDecimal("630"));

        // CFCE patronal : 950 000 * 3% = 28 500
        BigDecimal cfce = findLineAmount(result, PayslipLineType.CFCE);
        assertThat(cfce).isEqualByComparingTo(new BigDecimal("28500"));

        // IPM patronal : 250 000 * 3% = 7 500
        BigDecimal ipmPatronal = findLineAmount(result, PayslipLineType.IPM_EMPLOYER);
        assertThat(ipmPatronal).isEqualByComparingTo(new BigDecimal("7500"));

        // Total charges patronales = 36 288 + 18 648 + 4 410 + 630 + 28 500 + 7 500 = 95 976 FCFA
        assertThat(result.totalEmployerContributions()).isEqualByComparingTo(new BigDecimal("95976"));
    }

    @Test
    @DisplayName("Cas salarié non-cadre sous 432 000 FCFA (Brut 300 000 FCFA)")
    void shouldCalculateNonCadreUnderCeiling() {
        PayrollCalculator.CalculationResult result = calculator.calculate(
                new BigDecimal("300000"),
                false, // non-cadre
                "CELIBATAIRE",
                0,
                defaultParams,
                defaultTaxBrackets,
                Collections.emptyList()
        );

        // IPRES RG salarié : 300 000 * 5.6% = 16 800
        assertThat(findLineAmount(result, PayslipLineType.IPRES_RG_EMPLOYEE)).isEqualByComparingTo(new BigDecimal("16800"));
        // Pas de ligne IPRES RC
        assertThat(hasLine(result, PayslipLineType.IPRES_RC_EMPLOYEE)).isFalse();
        assertThat(hasLine(result, PayslipLineType.IPRES_RC_EMPLOYER)).isFalse();

        // IPM salarié : 250 000 * 3% = 7 500
        assertThat(findLineAmount(result, PayslipLineType.IPM)).isEqualByComparingTo(new BigDecimal("7500"));

        // Brut imposable = 300 000 - 16 800 - 7 500 = 275 700
        assertThat(result.taxableGross()).isEqualByComparingTo(new BigDecimal("275700"));

        // Frais pro : min(275 700 * 30% = 82 710, 75 000) = 75 000
        // Net imposable = 275 700 - 75 000 = 200 700
        assertThat(result.netTaxable()).isEqualByComparingTo(new BigDecimal("200700"));

        // IR mensuel : 37 210 FCFA
        assertThat(findLineAmount(result, PayslipLineType.TAX_IR)).isEqualByComparingTo(new BigDecimal("37210"));

        // Charges patronales :
        // IPRES RG : 300 000 * 8.4% = 25 200
        // CSS PF : 63 000 * 7% = 4 410
        // CSS AT : 63 000 * 1% = 630
        // CFCE : 300 000 * 3% = 9 000
        // IPM : 250 000 * 3% = 7 500
        // Total patronal = 25 200 + 4 410 + 630 + 9 000 + 7 500 = 46 740 FCFA
        assertThat(result.totalEmployerContributions()).isEqualByComparingTo(new BigDecimal("46740"));
    }

    @Test
    @DisplayName("Tests aux limites exactes de chaque tranche du barème annuel de l'IR")
    void shouldTestExactTaxBracketBoundaries() {
        BigDecimal parts = BigDecimal.ONE;

        // Tranche 1 : 0 à 630 000 annuel (mensuel net imposable 52 500) -> IR = 0
        BigDecimal irAt630k = calculator.calculateMonthlyIr(new BigDecimal("52500"), parts, defaultTaxBrackets);
        assertThat(irAt630k).isEqualByComparingTo(BigDecimal.ZERO);

        // Tranche 2 : Seuil 1 500 000 annuel (mensuel 125 000) : (1 500 000 - 630 000) * 20% = 174 000 / 12 = 14 500
        BigDecimal irAt1_5M = calculator.calculateMonthlyIr(new BigDecimal("125000"), parts, defaultTaxBrackets);
        assertThat(irAt1_5M).isEqualByComparingTo(new BigDecimal("14500"));

        // Tranche 3 : Seuil 4 000 000 annuel (mensuel 333 333.333333) :
        // Annuel 4 000 000 exact -> 174 000 + (2 500 000 * 30% = 750 000) = 924 000 / 12 = 77 000
        BigDecimal monthlyAt4M = new BigDecimal("4000000").divide(new BigDecimal("12"), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal irAt4M = calculator.calculateMonthlyIr(monthlyAt4M, parts, defaultTaxBrackets);
        assertThat(irAt4M).isEqualByComparingTo(new BigDecimal("77000"));

        // Tranche 4 : Seuil 8 000 000 annuel :
        // 924 000 + (4 000 000 * 35% = 1 400 000) = 2 324 000 / 12 = 193 666.666 -> 193 667
        BigDecimal monthlyAt8M = new BigDecimal("8000000").divide(new BigDecimal("12"), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal irAt8M = calculator.calculateMonthlyIr(monthlyAt8M, parts, defaultTaxBrackets);
        assertThat(irAt8M).isEqualByComparingTo(new BigDecimal("193667"));

        // Tranche 5 : Seuil 13 500 000 annuel :
        // 2 324 000 + (5 500 000 * 37% = 2 035 000) = 4 359 000 / 12 = 363 250
        BigDecimal monthlyAt13_5M = new BigDecimal("13500000").divide(new BigDecimal("12"), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal irAt13_5M = calculator.calculateMonthlyIr(monthlyAt13_5M, parts, defaultTaxBrackets);
        assertThat(irAt13_5M).isEqualByComparingTo(new BigDecimal("363250"));
    }

    @Test
    @DisplayName("Vérifie les parts fiscales et la réduction d'IR avec le quotient familial")
    void shouldCalculateTaxPartsAndFamilyQuotient() {
        // Célibataire sans enfant -> 1.0 part
        assertThat(calculator.calculateTaxParts("CELIBATAIRE", 0, defaultParams)).isEqualByComparingTo("1.0");

        // Marié sans enfant -> 1.5 parts
        assertThat(calculator.calculateTaxParts("MARIE", 0, defaultParams)).isEqualByComparingTo("1.5");

        // Marié 2 enfants -> 1.5 + 2 * 0.5 = 2.5 parts
        assertThat(calculator.calculateTaxParts("MARIE", 2, defaultParams)).isEqualByComparingTo("2.5");

        // Marié 10 enfants -> plafonné à 5.0 parts
        assertThat(calculator.calculateTaxParts("MARIE", 10, defaultParams)).isEqualByComparingTo("5.0");

        // Comparaison IR pour un net imposable de 830 876 FCFA :
        BigDecimal netTaxable = new BigDecimal("830876");
        BigDecimal ir1Part = calculator.calculateMonthlyIr(netTaxable, new BigDecimal("1.0"), defaultTaxBrackets);
        BigDecimal ir2Parts = calculator.calculateMonthlyIr(netTaxable, new BigDecimal("2.0"), defaultTaxBrackets);
        BigDecimal ir3Parts = calculator.calculateMonthlyIr(netTaxable, new BigDecimal("3.0"), defaultTaxBrackets);

        assertThat(ir1Part).isEqualByComparingTo(new BigDecimal("254424"));
        assertThat(ir2Parts).isLessThan(ir1Part);
        assertThat(ir3Parts).isLessThan(ir2Parts);
    }

    @Test
    @DisplayName("Test isolé de la TRIMF sur sa propre table de tranches annuelles")
    void shouldCalculateTrimfSeparately() {
        // Brut mensuel 40 000 -> annuel 480 000 -> Tranche 1 (900 / an) -> 900 / 12 = 75
        BigDecimal trimf1 = calculator.calculateMonthlyTrimf(new BigDecimal("40000"), defaultTrimfBrackets);
        assertThat(trimf1).isEqualByComparingTo(new BigDecimal("75"));

        // Brut mensuel 80 000 -> annuel 960 000 -> Tranche 2 (3 600 / an) -> 3 600 / 12 = 300
        BigDecimal trimf2 = calculator.calculateMonthlyTrimf(new BigDecimal("80000"), defaultTrimfBrackets);
        assertThat(trimf2).isEqualByComparingTo(new BigDecimal("300"));

        // Brut mensuel 150 000 -> annuel 1 800 000 -> Tranche 3 (4 800 / an) -> 4 800 / 12 = 400
        BigDecimal trimf3 = calculator.calculateMonthlyTrimf(new BigDecimal("150000"), defaultTrimfBrackets);
        assertThat(trimf3).isEqualByComparingTo(new BigDecimal("400"));

        // Brut mensuel 250 000 -> annuel 3 000 000 -> Tranche 4 (12 000 / an) -> 12 000 / 12 = 1 000
        BigDecimal trimf4 = calculator.calculateMonthlyTrimf(new BigDecimal("250000"), defaultTrimfBrackets);
        assertThat(trimf4).isEqualByComparingTo(new BigDecimal("1000"));

        // Brut mensuel 950 000 -> annuel 11 400 000 -> Tranche 5 (36 000 / an) -> 36 000 / 12 = 3 000
        BigDecimal trimf5 = calculator.calculateMonthlyTrimf(new BigDecimal("950000"), defaultTrimfBrackets);
        assertThat(trimf5).isEqualByComparingTo(new BigDecimal("3000"));
    }

    @Test
    @DisplayName("Vérifie la cohérence arithmétique : somme des lignes = totaux du bulletin")
    void shouldVerifyLinesSumEqualsTotals() {
        PayrollCalculator.CalculationResult result = calculator.calculate(
                new BigDecimal("950000"),
                true,
                "MARIE",
                2,
                defaultParams,
                defaultTaxBrackets,
                defaultTrimfBrackets
        );

        BigDecimal sumDeductions = result.lines().stream()
                .filter(l -> l.category() == PayslipLineCategory.DEDUCTION_SALARIALE)
                .map(PayrollCalculator.CalculatedLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(result.totalDeductions()).isEqualByComparingTo(sumDeductions);
        assertThat(result.grossSalary().subtract(result.totalDeductions())).isEqualByComparingTo(result.netSalary());

        BigDecimal sumEmployer = result.lines().stream()
                .filter(l -> l.category() == PayslipLineCategory.COTISATION_PATRONALE)
                .map(PayrollCalculator.CalculatedLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(result.totalEmployerContributions()).isEqualByComparingTo(sumEmployer);
    }

    @Test
    @DisplayName("Option PAYROLL_IPRES_RC_CADRES_ONLY désactivée : applique la complémentaire même aux non-cadres")
    void shouldApplyIpresRcWhenCadresOnlyFalse() {
        Map<String, String> params = new HashMap<>(defaultParams);
        params.put("PAYROLL_IPRES_RC_CADRES_ONLY", "false");

        PayrollCalculator.CalculationResult result = calculator.calculate(
                new BigDecimal("950000"),
                false, // non-cadre
                "CELIBATAIRE",
                0,
                params,
                defaultTaxBrackets,
                Collections.emptyList()
        );

        // Avec cadres_only = false, la tranche [432 000 - 950 000] est soumise à la RC
        assertThat(hasLine(result, PayslipLineType.IPRES_RC_EMPLOYEE)).isTrue();
        assertThat(findLineAmount(result, PayslipLineType.IPRES_RC_EMPLOYEE)).isEqualByComparingTo(new BigDecimal("12432"));
    }

    @Test
    @DisplayName("Rejette un salaire brut nul ou négatif")
    void shouldRejectInvalidGrossSalary() {
        assertThatThrownBy(() -> calculator.calculate(BigDecimal.ZERO, false, "CELIBATAIRE", 0, defaultParams, defaultTaxBrackets, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(new BigDecimal("-1000"), false, "CELIBATAIRE", 0, defaultParams, defaultTaxBrackets, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private BigDecimal findLineAmount(PayrollCalculator.CalculationResult result, PayslipLineType type) {
        return result.lines().stream()
                .filter(l -> l.lineType() == type)
                .map(PayrollCalculator.CalculatedLine::amount)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Ligne " + type + " non trouvée"));
    }

    private boolean hasLine(PayrollCalculator.CalculationResult result, PayslipLineType type) {
        return result.lines().stream().anyMatch(l -> l.lineType() == type);
    }
}

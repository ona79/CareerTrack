package com.careertrack.modules.payroll.service;

import com.careertrack.modules.payroll.entity.PayrollTaxBracket;
import com.careertrack.modules.payroll.entity.PayrollTrimfBracket;
import com.careertrack.modules.payroll.entity.PayslipLineCategory;
import com.careertrack.modules.payroll.entity.PayslipLineType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

/**
 * Moteur pur de calcul de paie en FCFA (sans accès base de données).
 * Toutes les opérations financières utilisent BigDecimal avec arrondi HALF_UP au franc près (0 décimale).
 */
@Component
public class PayrollCalculator {

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    public record CalculatedLine(
            PayslipLineType lineType,
            PayslipLineCategory category,
            String label,
            BigDecimal baseAmount,
            BigDecimal rate,
            BigDecimal amount
    ) {}

    public record CalculationResult(
            BigDecimal grossSalary,
            BigDecimal taxableGross,
            BigDecimal taxParts,
            BigDecimal netTaxable,
            BigDecimal totalDeductions,
            BigDecimal netSalary,
            BigDecimal totalEmployerContributions,
            List<CalculatedLine> lines
    ) {}

    public CalculationResult calculate(
            BigDecimal grossSalary,
            boolean isCadre,
            String maritalStatus,
            int dependentsCount,
            Map<String, String> params,
            List<PayrollTaxBracket> taxBrackets,
            List<PayrollTrimfBracket> trimfBrackets
    ) {
        if (grossSalary == null || grossSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le salaire brut doit être strictement positif");
        }

        List<CalculatedLine> lines = new ArrayList<>();

        // 1. Ligne Salaire de base (Gain brut)
        lines.add(new CalculatedLine(
                PayslipLineType.BASE_SALARY,
                PayslipLineCategory.GAIN_BRUT,
                "Salaire de Base Mensuel",
                grossSalary,
                BigDecimal.ONE,
                grossSalary
        ));

        // 2. Calcul du nombre de parts fiscales
        BigDecimal taxParts = calculateTaxParts(maritalStatus, dependentsCount, params);

        // 3. Cotisations Retraite IPRES Régime Général
        BigDecimal ipresRgCeiling = getParamDecimal(params, "IPRES_RG_CEILING", new BigDecimal("432000"));
        BigDecimal ipresRgEmployeeRate = getParamDecimal(params, "IPRES_RG_EMPLOYEE_RATE", new BigDecimal("0.056"));
        BigDecimal ipresRgEmployerRate = getParamDecimal(params, "IPRES_RG_EMPLOYER_RATE", new BigDecimal("0.084"));

        BigDecimal ipresRgBase = grossSalary.min(ipresRgCeiling);
        BigDecimal ipresRgEmployeeAmount = roundMoney(ipresRgBase.multiply(ipresRgEmployeeRate));
        BigDecimal ipresRgEmployerAmount = roundMoney(ipresRgBase.multiply(ipresRgEmployerRate));

        lines.add(new CalculatedLine(
                PayslipLineType.IPRES_RG_EMPLOYEE,
                PayslipLineCategory.DEDUCTION_SALARIALE,
                "Retraite IPRES Régime Général (5,6%)",
                ipresRgBase,
                ipresRgEmployeeRate,
                ipresRgEmployeeAmount
        ));

        lines.add(new CalculatedLine(
                PayslipLineType.IPRES_RG_EMPLOYER,
                PayslipLineCategory.COTISATION_PATRONALE,
                "Retraite IPRES Régime Général Patronale (8,4%)",
                ipresRgBase,
                ipresRgEmployerRate,
                ipresRgEmployerAmount
        ));

        // 4. Cotisations Retraite IPRES Régime Complémentaire Cadres
        boolean cadresOnly = getParamBoolean(params, "PAYROLL_IPRES_RC_CADRES_ONLY", true);
        boolean applyIpresRc = !cadresOnly || isCadre;

        BigDecimal ipresRcCeiling = getParamDecimal(params, "IPRES_RC_CEILING", new BigDecimal("1296000"));
        BigDecimal ipresRcEmployeeRate = getParamDecimal(params, "IPRES_RC_EMPLOYEE_RATE", new BigDecimal("0.024"));
        BigDecimal ipresRcEmployerRate = getParamDecimal(params, "IPRES_RC_EMPLOYER_RATE", new BigDecimal("0.036"));

        BigDecimal ipresRcEmployeeAmount = BigDecimal.ZERO;
        BigDecimal ipresRcEmployerAmount = BigDecimal.ZERO;

        if (applyIpresRc && grossSalary.compareTo(ipresRgCeiling) > 0) {
            BigDecimal ipresRcBase = grossSalary.min(ipresRcCeiling).subtract(ipresRgCeiling);
            ipresRcEmployeeAmount = roundMoney(ipresRcBase.multiply(ipresRcEmployeeRate));
            ipresRcEmployerAmount = roundMoney(ipresRcBase.multiply(ipresRcEmployerRate));

            lines.add(new CalculatedLine(
                    PayslipLineType.IPRES_RC_EMPLOYEE,
                    PayslipLineCategory.DEDUCTION_SALARIALE,
                    "Retraite IPRES Cadres (2,4%)",
                    ipresRcBase,
                    ipresRcEmployeeRate,
                    ipresRcEmployeeAmount
            ));

            lines.add(new CalculatedLine(
                    PayslipLineType.IPRES_RC_EMPLOYER,
                    PayslipLineCategory.COTISATION_PATRONALE,
                    "Retraite IPRES Cadres Patronale (3,6%)",
                    ipresRcBase,
                    ipresRcEmployerRate,
                    ipresRcEmployerAmount
            ));
        }

        // 5. Caisse de Sécurité Sociale (CSS) - 100% Patronale
        BigDecimal cssCeiling = getParamDecimal(params, "CSS_CEILING", new BigDecimal("63000"));
        BigDecimal cssBase = grossSalary.min(cssCeiling);

        BigDecimal cssPfRate = getParamDecimal(params, "CSS_PF_EMPLOYER_RATE", new BigDecimal("0.070"));
        BigDecimal cssPfAmount = roundMoney(cssBase.multiply(cssPfRate));

        BigDecimal cssAtRate = getParamDecimal(params, "CSS_AT_EMPLOYER_RATE", new BigDecimal("0.010"));
        BigDecimal cssAtAmount = roundMoney(cssBase.multiply(cssAtRate));

        lines.add(new CalculatedLine(
                PayslipLineType.CSS_PF,
                PayslipLineCategory.COTISATION_PATRONALE,
                "CSS Prestations Familiales (7%)",
                cssBase,
                cssPfRate,
                cssPfAmount
        ));

        lines.add(new CalculatedLine(
                PayslipLineType.CSS_AT,
                PayslipLineCategory.COTISATION_PATRONALE,
                "CSS Accidents du Travail (1%)",
                cssBase,
                cssAtRate,
                cssAtAmount
        ));

        // 6. CFCE (Contribution Forfaitaire à la Charge des Employeurs) - Brut total non plafonné
        BigDecimal cfceRate = getParamDecimal(params, "CFCE_EMPLOYER_RATE", new BigDecimal("0.030"));
        BigDecimal cfceAmount = roundMoney(grossSalary.multiply(cfceRate));

        lines.add(new CalculatedLine(
                PayslipLineType.CFCE,
                PayslipLineCategory.COTISATION_PATRONALE,
                "Contribution Forfaitaire Employeur CFCE (3%)",
                grossSalary,
                cfceRate,
                cfceAmount
        ));

        // 7. IPM (Institut de Prévoyance Maladie)
        BigDecimal ipmCeiling = getParamDecimal(params, "IPM_CEILING", new BigDecimal("250000"));
        BigDecimal ipmBase = grossSalary.min(ipmCeiling);

        BigDecimal ipmEmployeeRate = getParamDecimal(params, "IPM_EMPLOYEE_RATE", new BigDecimal("0.030"));
        BigDecimal ipmEmployerRate = getParamDecimal(params, "IPM_EMPLOYER_RATE", new BigDecimal("0.030"));

        BigDecimal ipmEmployeeAmount = roundMoney(ipmBase.multiply(ipmEmployeeRate));
        BigDecimal ipmEmployerAmount = roundMoney(ipmBase.multiply(ipmEmployerRate));

        lines.add(new CalculatedLine(
                PayslipLineType.IPM,
                PayslipLineCategory.DEDUCTION_SALARIALE,
                "Cotisation Mutuelle Santé IPM (3%)",
                ipmBase,
                ipmEmployeeRate,
                ipmEmployeeAmount
        ));

        lines.add(new CalculatedLine(
                PayslipLineType.IPM_EMPLOYER,
                PayslipLineCategory.COTISATION_PATRONALE,
                "Cotisation Mutuelle Santé IPM Patronale (3%)",
                ipmBase,
                ipmEmployerRate,
                ipmEmployerAmount
        ));

        // 8. Base imposable (Brut imposable)
        boolean ipmDeductible = getParamBoolean(params, "PAYROLL_IPM_DEDUCTIBLE_FROM_IR", true);
        BigDecimal deductibleSocial = ipresRgEmployeeAmount.add(ipresRcEmployeeAmount);
        if (ipmDeductible) {
            deductibleSocial = deductibleSocial.add(ipmEmployeeAmount);
        }

        BigDecimal taxableGross = grossSalary.subtract(deductibleSocial);
        if (taxableGross.compareTo(BigDecimal.ZERO) < 0) {
            taxableGross = BigDecimal.ZERO;
        }

        // 9. Frais professionnels (Abattement 30% plafonné à 75 000 FCFA/mois)
        BigDecimal fraisProRate = getParamDecimal(params, "FRAIS_PRO_RATE", new BigDecimal("0.30"));
        BigDecimal fraisProCeiling = getParamDecimal(params, "FRAIS_PRO_MONTHLY_CEILING", new BigDecimal("75000"));
        BigDecimal fraisProAmount = taxableGross.multiply(fraisProRate).min(fraisProCeiling);

        BigDecimal netTaxable = taxableGross.subtract(fraisProAmount);
        if (netTaxable.compareTo(BigDecimal.ZERO) < 0) {
            netTaxable = BigDecimal.ZERO;
        }

        // 10. Impôt sur le Revenu (IR) – Calcul annuel exact avec quotient familial
        BigDecimal monthlyIr = calculateMonthlyIr(netTaxable, taxParts, taxBrackets);
        if (monthlyIr.compareTo(BigDecimal.ZERO) > 0) {
            // Taux moyen apparent pour affichage
            BigDecimal effectiveRate = netTaxable.compareTo(BigDecimal.ZERO) > 0
                    ? monthlyIr.divide(netTaxable, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            lines.add(new CalculatedLine(
                    PayslipLineType.TAX_IR,
                    PayslipLineCategory.DEDUCTION_SALARIALE,
                    "Impôt sur le Revenu (IR)",
                    netTaxable,
                    effectiveRate,
                    monthlyIr
            ));
        }

        // 11. TRIMF (Taxe Représentative de l'Impôt du Minimum Fiscal)
        BigDecimal monthlyTrimf = calculateMonthlyTrimf(grossSalary, trimfBrackets);
        if (monthlyTrimf.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new CalculatedLine(
                    PayslipLineType.TRIMF,
                    PayslipLineCategory.DEDUCTION_SALARIALE,
                    "TRIMF (Minimum Fiscal)",
                    grossSalary,
                    BigDecimal.ZERO,
                    monthlyTrimf
            ));
        }

        // 12. Totaux
        BigDecimal totalDeductions = lines.stream()
                .filter(l -> l.category() == PayslipLineCategory.DEDUCTION_SALARIALE)
                .map(CalculatedLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netSalary = grossSalary.subtract(totalDeductions);

        BigDecimal totalEmployer = lines.stream()
                .filter(l -> l.category() == PayslipLineCategory.COTISATION_PATRONALE)
                .map(CalculatedLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CalculationResult(
                grossSalary,
                taxableGross,
                taxParts,
                netTaxable,
                totalDeductions,
                netSalary,
                totalEmployer,
                Collections.unmodifiableList(lines)
        );
    }

    public BigDecimal calculateTaxParts(String maritalStatus, int dependentsCount, Map<String, String> params) {
        BigDecimal baseParts;
        if (maritalStatus != null && "MARIE".equalsIgnoreCase(maritalStatus.trim())) {
            baseParts = getParamDecimal(params, "TAX_PARTS_MARRIED", new BigDecimal("1.5"));
        } else {
            baseParts = getParamDecimal(params, "TAX_PARTS_SINGLE", new BigDecimal("1.0"));
        }

        BigDecimal perDependent = getParamDecimal(params, "TAX_PARTS_PER_DEPENDENT", new BigDecimal("0.5"));
        BigDecimal maxParts = getParamDecimal(params, "TAX_PARTS_MAX", new BigDecimal("5.0"));

        BigDecimal dependentParts = perDependent.multiply(BigDecimal.valueOf(Math.max(0, dependentsCount)));
        return baseParts.add(dependentParts).min(maxParts);
    }

    public BigDecimal calculateMonthlyIr(BigDecimal netTaxableMonthly, BigDecimal taxParts, List<PayrollTaxBracket> brackets) {
        if (netTaxableMonthly == null || netTaxableMonthly.compareTo(BigDecimal.ZERO) <= 0 || brackets == null || brackets.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal parts = (taxParts != null && taxParts.compareTo(BigDecimal.ZERO) > 0) ? taxParts : BigDecimal.ONE;

        // Revenu annuel imposable
        BigDecimal annualTaxable = netTaxableMonthly.multiply(TWELVE);

        // Quotient familial
        BigDecimal quotient = annualTaxable.divide(parts, MC);

        // Calcul de l'impôt annuel sur le quotient par tranches
        BigDecimal taxOnQuotient = BigDecimal.ZERO;

        for (PayrollTaxBracket bracket : brackets) {
            BigDecimal min = bracket.getMinAnnualIncome();
            BigDecimal max = bracket.getMaxAnnualIncome();
            BigDecimal rate = bracket.getRate();

            if (quotient.compareTo(min) > 0) {
                BigDecimal upper = (max != null) ? quotient.min(max) : quotient;
                BigDecimal taxableInBracket = upper.subtract(min);
                taxOnQuotient = taxOnQuotient.add(taxableInBracket.multiply(rate));
            }
        }

        // Impôt annuel total pour le foyer = impôt sur quotient * parts
        BigDecimal totalAnnualTax = taxOnQuotient.multiply(parts);

        // Retenue mensuelle = impôt annuel / 12, arrondi HALF_UP au franc
        return totalAnnualTax.divide(TWELVE, 0, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateMonthlyTrimf(BigDecimal grossSalaryMonthly, List<PayrollTrimfBracket> brackets) {
        if (grossSalaryMonthly == null || grossSalaryMonthly.compareTo(BigDecimal.ZERO) <= 0 || brackets == null || brackets.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal annualGross = grossSalaryMonthly.multiply(TWELVE);

        for (PayrollTrimfBracket bracket : brackets) {
            BigDecimal min = bracket.getMinAnnualGross();
            BigDecimal max = bracket.getMaxAnnualGross();

            boolean minOk = annualGross.compareTo(min) >= 0;
            boolean maxOk = (max == null) || annualGross.compareTo(max) < 0;

            if (minOk && maxOk) {
                return bracket.getAnnualAmount().divide(TWELVE, 0, RoundingMode.HALF_UP);
            }
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal roundMoney(BigDecimal val) {
        return val.setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal getParamDecimal(Map<String, String> params, String key, BigDecimal defaultVal) {
        if (params == null || !params.containsKey(key)) {
            return defaultVal;
        }
        try {
            return new BigDecimal(params.get(key).trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private boolean getParamBoolean(Map<String, String> params, String key, boolean defaultVal) {
        if (params == null || !params.containsKey(key)) {
            return defaultVal;
        }
        return Boolean.parseBoolean(params.get(key).trim());
    }
}

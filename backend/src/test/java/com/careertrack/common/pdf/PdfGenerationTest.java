package com.careertrack.common.pdf;

import com.careertrack.modules.employee.entity.*;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.entity.PayslipLine;
import com.careertrack.modules.payroll.entity.PayslipLineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PdfGenerationTest {

    private final PdfGeneratorService pdfGeneratorService = new PdfGeneratorService();

    @Test
    @DisplayName("Génère une attestation de travail valide au format PDF avec iText 7")
    void shouldGenerateWorkCertificatePdf() {
        Department dept = Department.builder()
                .name("Ingénierie & Systèmes")
                .code("DEP-TECH")
                .build();

        Position pos = Position.builder()
                .title("Développeur Fullstack")
                .code("POS-DEV")
                .department(dept)
                .build();

        Employee emp = Employee.builder()
                .id(UUID.randomUUID())
                .employeeNumber("EMP-004")
                .firstName("Ousmane")
                .lastName("Diallo")
                .nationalId("1 996 1996 00321")
                .hireDate(LocalDate.of(2022, 6, 1))
                .department(dept)
                .position(pos)
                .build();

        Contract contract = Contract.builder()
                .contractType(ContractType.CDI)
                .grossSalary(new BigDecimal("950000"))
                .startDate(LocalDate.of(2022, 6, 1))
                .build();

        byte[] pdfBytes = pdfGeneratorService.generateWorkCertificate(emp, contract);

        assertThat(pdfBytes).isNotEmpty();
        // Vérifie le header PDF standard %PDF-
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Génère un bulletin de paie valide avec cotisations sociales sénégalaises en FCFA")
    void shouldGeneratePayslipPdf() {
        Employee emp = Employee.builder()
                .id(UUID.randomUUID())
                .employeeNumber("EMP-004")
                .firstName("Ousmane")
                .lastName("Diallo")
                .nationalId("1 996 1996 00321")
                .hireDate(LocalDate.of(2022, 6, 1))
                .build();

        Payslip payslip = Payslip.builder()
                .id(UUID.randomUUID())
                .employee(emp)
                .periodMonth(9)
                .periodYear(2026)
                .paymentDate(LocalDate.of(2026, 9, 30))
                .grossSalary(new BigDecimal("950000"))
                .netSalary(new BigDecimal("786800"))
                .totalDeductions(new BigDecimal("163200"))
                .lines(List.of(
                        PayslipLine.builder().label("Salaire de base").baseAmount(new BigDecimal("950000")).rate(BigDecimal.ONE).amount(new BigDecimal("950000")).lineType(PayslipLineType.BASE_SALARY).build(),
                        PayslipLine.builder().label("IPRES Salariale (5.6%)").baseAmount(new BigDecimal("950000")).rate(new BigDecimal("0.056")).amount(new BigDecimal("53200")).lineType(PayslipLineType.IPRES_EMPLOYEE).build(),
                        PayslipLine.builder().label("IPM Santé").baseAmount(new BigDecimal("950000")).rate(new BigDecimal("0.015")).amount(new BigDecimal("14250")).lineType(PayslipLineType.IPM).build(),
                        PayslipLine.builder().label("Impôt sur le Revenu (IR)").baseAmount(new BigDecimal("950000")).rate(new BigDecimal("0.1008")).amount(new BigDecimal("95750")).lineType(PayslipLineType.TAX_IR).build()
                ))
                .build();

        byte[] pdfBytes = pdfGeneratorService.generatePayslipPdf(payslip);

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Génère un contrat de travail (modèle de démonstration) au format PDF")
    void shouldGenerateContractPdf() {
        Department dept = Department.builder()
                .name("Ingénierie & Systèmes")
                .code("DEP-TECH")
                .build();

        Position pos = Position.builder()
                .title("Développeur Fullstack Senior")
                .code("POS-DEV-SR")
                .department(dept)
                .build();

        Employee emp = Employee.builder()
                .id(UUID.randomUUID())
                .employeeNumber("EMP-004")
                .firstName("Ousmane")
                .lastName("Diallo")
                .nationalId("1 996 1996 00321")
                .address("Liberté 6, Dakar")
                .hireDate(LocalDate.of(2022, 6, 1))
                .department(dept)
                .position(pos)
                .build();

        Contract contract = Contract.builder()
                .contractType(ContractType.CDI)
                .grossSalary(new BigDecimal("950000"))
                .startDate(LocalDate.of(2022, 6, 1))
                .build();

        byte[] pdfBytes = pdfGeneratorService.generateEmploymentContract(emp, contract);

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
    }
}

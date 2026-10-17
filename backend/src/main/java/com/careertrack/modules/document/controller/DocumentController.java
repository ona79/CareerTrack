package com.careertrack.modules.document.controller;

import com.careertrack.common.pdf.PdfGeneratorService;
import com.careertrack.modules.employee.entity.*;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.entity.PayslipLine;
import com.careertrack.modules.payroll.entity.PayslipLineType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents & PDF", description = "Génération de documents PDF officiels (Attestations, Bulletins de paie, Contrats)")
@SecurityRequirement(name = "BearerAuth")
public class DocumentController {

    private final PdfGeneratorService pdfGeneratorService;

    @GetMapping("/attestation/exemple")
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER', 'EMPLOYE', 'DIRECTION')")
    @Operation(summary = "Exemple d'attestation de travail PDF", description = "Génère et retourne un document PDF d'attestation de travail aux normes sénégalaises")
    public ResponseEntity<byte[]> getSampleWorkCertificate() {
        Department dept = Department.builder()
                .name("Ingénierie & Systèmes")
                .code("DEP-TECH")
                .build();

        Position pos = Position.builder()
                .title("Développeur Fullstack")
                .code("POS-DEV")
                .department(dept)
                .build();

        Employee employee = Employee.builder()
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

        byte[] pdfBytes = pdfGeneratorService.generateWorkCertificate(employee, contract);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"attestation-travail-exemple.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/bulletin-paie/exemple")
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER', 'EMPLOYE', 'DIRECTION')")
    @Operation(summary = "Exemple de bulletin de paie PDF", description = "Génère et retourne un bulletin de paie PDF avec cotisations IPRES, CSS, IPM en FCFA")
    public ResponseEntity<byte[]> getSamplePayslipPdf() {
        Employee employee = Employee.builder()
                .id(UUID.randomUUID())
                .employeeNumber("EMP-004")
                .firstName("Ousmane")
                .lastName("Diallo")
                .nationalId("1 996 1996 00321")
                .hireDate(LocalDate.of(2022, 6, 1))
                .build();

        Payslip payslip = Payslip.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .periodMonth(9)
                .periodYear(2026)
                .paymentDate(LocalDate.of(2026, 9, 30))
                .grossSalary(new BigDecimal("950000"))
                .netSalary(new BigDecimal("786800"))
                .totalDeductions(new BigDecimal("163200"))
                .lines(List.of(
                        PayslipLine.builder().label("Salaire de Base Mensuel").baseAmount(new BigDecimal("950000")).rate(BigDecimal.ONE).amount(new BigDecimal("950000")).lineType(PayslipLineType.BASE_SALARY).build(),
                        PayslipLine.builder().label("Retraite IPRES Salariale (5.6%)").baseAmount(new BigDecimal("950000")).rate(new BigDecimal("0.056")).amount(new BigDecimal("53200")).lineType(PayslipLineType.IPRES_EMPLOYEE).build(),
                        PayslipLine.builder().label("Cotisation Mutuelle Santé (IPM)").baseAmount(new BigDecimal("950000")).rate(new BigDecimal("0.015")).amount(new BigDecimal("14250")).lineType(PayslipLineType.IPM).build(),
                        PayslipLine.builder().label("Impôt sur le Revenu (IR)").baseAmount(new BigDecimal("950000")).rate(new BigDecimal("0.1008")).amount(new BigDecimal("95750")).lineType(PayslipLineType.TAX_IR).build()
                ))
                .build();

        byte[] pdfBytes = pdfGeneratorService.generatePayslipPdf(payslip);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"bulletin-paie-exemple.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}

package com.careertrack.modules.document.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.common.pdf.PdfGeneratorService;
import com.careertrack.modules.document.dto.DocumentResponseDto;
import com.careertrack.modules.document.service.EmployeeDocumentService;
import com.careertrack.modules.employee.entity.*;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.entity.PayslipLine;
import com.careertrack.modules.payroll.entity.PayslipLineType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents & PDF", description = "Gestion documentaire (envoi, téléchargement, suppression) et génération de documents PDF officiels")
@SecurityRequirement(name = "BearerAuth")
public class DocumentController {

    private final PdfGeneratorService pdfGeneratorService;
    private final com.careertrack.modules.document.service.DocumentService documentService;
    private final EmployeeDocumentService employeeDocumentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@documentSecurity.canUploadDocument(#employeeId, #type)")
    @Operation(summary = "Téléverser un document pour un employé", description = "Accessible à la DRH (tous types) et à l'employé pour lui-même (CNI, DIPLOME, AUTRE - hors CONTRAT)")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("employeeId") UUID employeeId,
            @RequestParam("type") DocumentType type,
            @RequestParam("title") String title) {

        DocumentResponseDto response = employeeDocumentService.uploadDocument(file, employeeId, type, title);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document téléversé avec succès", response));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("@documentSecurity.canListEmployeeDocuments(#employeeId)")
    @Operation(summary = "Lister les documents d'un employé (métadonnées uniquement)", description = "Accessible à la DRH et à l'employé pour ses propres documents. Aucun contenu binaire renvoyé.")
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> getEmployeeDocuments(@PathVariable UUID employeeId) {
        List<DocumentResponseDto> documents = employeeDocumentService.getDocumentsByEmployee(employeeId);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("@documentSecurity.canDownloadDocument(#id)")
    @Operation(summary = "Télécharger un document téléversé", description = "Accessible à la DRH et au collaborateur concerné")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable UUID id) {
        EmployeeDocument metadata = employeeDocumentService.getDocumentMetadata(id);
        byte[] data = employeeDocumentService.downloadDocument(id);

        String disposition = ContentDisposition.attachment()
                .filename(metadata.getFileName(), StandardCharsets.UTF_8)
                .build()
                .toString();

        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        if (metadata.getMimeType() != null && !metadata.getMimeType().isBlank()) {
            try {
                contentType = MediaType.parseMediaType(metadata.getMimeType());
            } catch (Exception ignored) {
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .contentType(contentType)
                .body(data);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@documentSecurity.canDeleteDocument(#id)")
    @Operation(summary = "Supprimer un document téléversé", description = "Accessible à la DRH ou à l'employé pour les documents qu'il a lui-même téléversés")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID id) {
        employeeDocumentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success("Document supprimé avec succès", null));
    }

    @GetMapping("/attestation/{employeeId}")
    @PreAuthorize("@documentSecurity.canAccessWorkCertificate(#employeeId)")
    @Operation(summary = "Télécharger l'attestation de travail officielle en PDF", description = "Accessible à la DRH, à l'employé pour lui-même et au Manager pour son équipe directe")
    public ResponseEntity<byte[]> getWorkCertificatePdf(@PathVariable UUID employeeId) {
        byte[] pdfBytes = documentService.generateWorkCertificate(employeeId);
        String filename = documentService.getWorkCertificateFilename(employeeId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/payslip/{payslipId}")
    @PreAuthorize("@documentSecurity.canAccessPayslip(#payslipId)")
    @Operation(summary = "Télécharger le bulletin de paie officiel en PDF", description = "Accessible à la DRH et à l'employé concerné uniquement")
    public ResponseEntity<byte[]> getPayslipPdf(@PathVariable UUID payslipId) {
        byte[] pdfBytes = documentService.generatePayslip(payslipId);
        String filename = documentService.getPayslipFilename(payslipId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/contract/{employeeId}")
    @PreAuthorize("@documentSecurity.canAccessContract(#employeeId)")
    @Operation(summary = "Télécharger le contrat de travail officiel en PDF", description = "Accessible à la DRH et à l'employé concerné uniquement")
    public ResponseEntity<byte[]> getEmploymentContractPdf(@PathVariable UUID employeeId) {
        byte[] pdfBytes = documentService.generateEmploymentContract(employeeId);
        String filename = documentService.getContractFilename(employeeId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

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

package com.careertrack.modules.payroll.controller;

import com.careertrack.common.exception.BadRequestException;
import com.careertrack.common.payload.ApiResponse;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.document.service.DocumentService;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.payroll.dto.*;
import com.careertrack.modules.payroll.entity.PayslipStatus;
import com.careertrack.modules.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll")
@Tag(name = "Paie & Salaires", description = "Gestion des bulletins de salaire, cotisations et masse salariale")
public class PayrollController {

    private final PayrollService payrollService;
    private final EmployeeRepository employeeRepository;
    private final DocumentService documentService;

    public PayrollController(PayrollService payrollService,
                             EmployeeRepository employeeRepository,
                             DocumentService documentService) {
        this.payrollService = payrollService;
        this.employeeRepository = employeeRepository;
        this.documentService = documentService;
    }

    @GetMapping("/payslips")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Lister les bulletins de paie par période (YYYY-MM)", description = "Accessible à la DRH uniquement")
    public ResponseEntity<ApiResponse<List<PayslipResponseDto>>> getPayslipsByPeriod(@RequestParam String period) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayslipsByPeriod(period)));
    }

    @GetMapping("/my-payslips")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lister les bulletins de l'employé connecté", description = "Accessible à tout collaborateur connecté pour consulter ses propres bulletins")
    public ResponseEntity<ApiResponse<List<PayslipResponseDto>>> getMyPayslips(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        Optional<Employee> emp = employeeRepository.findByUserId(principal.getId());
        if (emp.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayslipsByEmployee(emp.get().getId())));
    }

    @GetMapping("/payslips/employee/{employeeId}")
    @PreAuthorize("@payrollSecurity.canAccessEmployeePayslips(#employeeId)")
    @Operation(summary = "Lister l'historique des bulletins d'un collaborateur", description = "Accessible au collaborateur lui-même et à la DRH")
    public ResponseEntity<ApiResponse<List<PayslipResponseDto>>> getPayslipsByEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayslipsByEmployee(employeeId)));
    }

    @GetMapping("/payslips/{id}")
    @PreAuthorize("@payrollSecurity.canAccessPayslip(#id)")
    @Operation(summary = "Obtenir le détail d'un bulletin de paie avec toutes les lignes", description = "Accessible au collaborateur concerné et à la DRH")
    public ResponseEntity<ApiResponse<PayslipResponseDto>> getPayslipById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayslipById(id)));
    }

    @GetMapping(value = "/payslips/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@payrollSecurity.canAccessPayslip(#id)")
    @Operation(summary = "Télécharger le bulletin de paie au format PDF", description = "Accessible à la DRH et au titulaire du bulletin")
    public ResponseEntity<byte[]> getPayslipPdf(@PathVariable UUID id) {
        byte[] pdfBytes = documentService.generatePayslip(id);
        String filename = documentService.getPayslipFilename(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Générer la paie mensuelle globale pour tous les salariés actifs", description = "Calcul automatique des cotisations sociales et retenues fiscales")
    public ResponseEntity<ApiResponse<PayrollGenerationResultDto>> generatePayroll(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        String resolvedPeriod = resolvePeriod(period, year, month);
        PayrollGenerationResultDto result = payrollService.generatePayrollForPeriod(resolvedPeriod);
        return ResponseEntity.ok(ApiResponse.success("Paie générée avec succès pour " + resolvedPeriod, result));
    }

    @PatchMapping("/payslips/{id}/status")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Modifier le statut d'un bulletin de paie (VALIDATED, PAID, CANCELLED)", description = "Réservé à la DRH")
    public ResponseEntity<ApiResponse<PayslipResponseDto>> updateStatus(@PathVariable UUID id, @RequestParam PayslipStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour", payrollService.updateStatus(id, status)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('DRH', 'DIRECTION')")
    @Operation(summary = "Statistiques globales et ventilation de la masse salariale", description = "Accessible à la DRH et à la Direction Générale. Respecte l'anonymat (< 3 salariés groupés dans Autres)")
    public ResponseEntity<ApiResponse<PayrollStatsResponseDto>> getPayrollStats(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        String resolvedPeriod = resolvePeriod(period, year, month);
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayrollStats(resolvedPeriod)));
    }

    @GetMapping("/parameters")
    @PreAuthorize("hasAnyRole('DRH', 'DIRECTION')")
    @Operation(summary = "Consulter les paramètres légaux et taux de paie en lecture seule", description = "Accessible à la DRH et à la Direction")
    public ResponseEntity<ApiResponse<List<PayrollParameterResponseDto>>> getPayrollParameters() {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayrollParameters()));
    }

    private String resolvePeriod(String period, Integer year, Integer month) {
        if (period != null && !period.isBlank()) {
            return period.trim();
        }
        if (year != null && month != null) {
            return String.format("%d-%02d", year, month);
        }
        throw new BadRequestException("La période est requise (format 'YYYY-MM' ou paramètres 'year' et 'month')");
    }
}

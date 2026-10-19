package com.careertrack.modules.payroll.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.payroll.dto.*;
import com.careertrack.modules.payroll.entity.PayslipStatus;
import com.careertrack.modules.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll")
@Tag(name = "Paie & Salaires", description = "Gestion des bulletins de salaire, cotisations et masse salariale")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping("/payslips")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Lister les bulletins de paie par période (YYYY-MM)", description = "Accessible à la DRH uniquement")
    public ResponseEntity<ApiResponse<List<PayslipResponseDto>>> getPayslipsByPeriod(@RequestParam String period) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayslipsByPeriod(period)));
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

    @PostMapping("/generate")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Générer la paie mensuelle globale pour tous les salariés actifs", description = "Calcul automatique des cotisations sociales et retenues fiscales")
    public ResponseEntity<ApiResponse<PayrollGenerationResultDto>> generatePayroll(@RequestParam String period) {
        PayrollGenerationResultDto result = payrollService.generatePayrollForPeriod(period);
        return ResponseEntity.ok(ApiResponse.success("Paie générée avec succès pour " + period, result));
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
    public ResponseEntity<ApiResponse<PayrollStatsResponseDto>> getPayrollStats(@RequestParam String period) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayrollStats(period)));
    }

    @GetMapping("/parameters")
    @PreAuthorize("hasAnyRole('DRH', 'DIRECTION')")
    @Operation(summary = "Consulter les paramètres légaux et taux de paie en lecture seule", description = "Accessible à la DRH et à la Direction")
    public ResponseEntity<ApiResponse<List<PayrollParameterResponseDto>>> getPayrollParameters() {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayrollParameters()));
    }
}

package com.careertrack.modules.payroll.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.payroll.dto.PayslipResponseDto;
import com.careertrack.modules.payroll.entity.PayslipStatus;
import com.careertrack.modules.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
@Tag(name = "Paie & Salaires", description = "Gestion des bulletins de salaire et cotisations IPRES/CSS")
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping("/payslips")
    @PreAuthorize("hasAnyRole('DRH', 'DIRECTION')")
    @Operation(summary = "Lister les bulletins de paie par période (YYYY-MM)", description = "Accessible à la DRH et Direction")
    public ResponseEntity<ApiResponse<List<PayslipResponseDto>>> getPayslipsByPeriod(@RequestParam String period) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayslipsByPeriod(period)));
    }

    @GetMapping("/payslips/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('DRH', 'DIRECTION', 'EMPLOYE')")
    @Operation(summary = "Lister l'historique des bulletins d'un collaborateur", description = "Accessible au collaborateur lui-même, à la DRH et Direction")
    public ResponseEntity<ApiResponse<List<PayslipResponseDto>>> getPayslipsByEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayslipsByEmployee(employeeId)));
    }

    @GetMapping("/payslips/{id}")
    @PreAuthorize("hasAnyRole('DRH', 'DIRECTION', 'EMPLOYE')")
    @Operation(summary = "Obtenir le détail d'un bulletin de paie avec toutes les lignes de déduction", description = "Accessible aux rôles autorisés")
    public ResponseEntity<ApiResponse<PayslipResponseDto>> getPayslipById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayslipById(id)));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Générer la paie mensuelle globale pour tous les salariés", description = "Calcul automatique des retenues IPRES et charges CSS")
    public ResponseEntity<ApiResponse<List<PayslipResponseDto>>> generatePayroll(@RequestParam String period) {
        return ResponseEntity.ok(ApiResponse.success("Paie générée avec succès pour " + period, payrollService.generatePayrollForPeriod(period)));
    }

    @PatchMapping("/payslips/{id}/status")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Modifier le statut d'un bulletin de paie (DRAFT, VALIDATED, PAID)", description = "Réservé à la DRH")
    public ResponseEntity<ApiResponse<PayslipResponseDto>> updateStatus(@PathVariable UUID id, @RequestParam PayslipStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour", payrollService.updateStatus(id, status)));
    }
}

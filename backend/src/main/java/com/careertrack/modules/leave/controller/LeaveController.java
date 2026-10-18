package com.careertrack.modules.leave.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.leave.dto.*;
import com.careertrack.modules.leave.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
@Tag(name = "Congés & Absences", description = "Gestion des demandes de congés, soldes et calendrier d'équipe")
@SecurityRequirement(name = "BearerAuth")
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'DRH', 'DIRECTION')")
    @Operation(summary = "Lister toutes les demandes de congés", description = "Retourne l'ensemble des demandes de congés de l'entreprise")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponseDto>>> getAllLeaves() {
        List<LeaveRequestResponseDto> response = leaveService.getAllLeaveRequests();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'DRH')")
    @Operation(summary = "Soumettre une demande de congé", description = "Crée une nouvelle demande de congé avec vérification du solde")
    public ResponseEntity<ApiResponse<LeaveRequestResponseDto>> createLeaveRequest(
            @Valid @RequestBody LeaveRequestCreateDto createDto) {
        LeaveRequestResponseDto response = leaveService.createLeaveRequest(createDto);
        return new ResponseEntity<>(ApiResponse.success("Demande de congé enregistrée", response), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('MANAGER', 'DRH')")
    @Operation(summary = "Valider ou refuser un congé", description = "Validation N+1 ou DRH avec décompte automatique du solde")
    public ResponseEntity<ApiResponse<LeaveRequestResponseDto>> validateLeave(
            @PathVariable UUID id,
            @Valid @RequestBody LeaveApprovalDto approvalDto,
            Authentication authentication
    ) {
        LeaveRequestResponseDto response = leaveService.approveOrRejectLeave(id, approvalDto, authentication);
        return ResponseEntity.ok(ApiResponse.success("Statut de la demande mis à jour", response));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("@leaveSecurity.canAccessEmployeeLeaves(#employeeId)")
    @Operation(summary = "Historique des demandes d'un employé", description = "Retourne toutes les demandes de congés d'un collaborateur")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponseDto>>> getEmployeeLeaves(@PathVariable UUID employeeId) {
        List<LeaveRequestResponseDto> response = leaveService.getEmployeeLeaveRequests(employeeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/manager/{managerId}/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'DRH')")
    @Operation(summary = "Demandes en attente pour un manager", description = "Retourne les demandes de congés en attente d'approbation N+1")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponseDto>>> getPendingLeavesForManager(@PathVariable UUID managerId) {
        List<LeaveRequestResponseDto> response = leaveService.getPendingRequestsForManager(managerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/balances/{employeeId}")
    @PreAuthorize("@leaveSecurity.canAccessEmployeeBalances(#employeeId)")
    @Operation(summary = "Soldes de congés d'un employé", description = "Retourne le compteur de jours acquis, utilisés et restants")
    public ResponseEntity<ApiResponse<List<LeaveBalanceDto>>> getBalances(
            @PathVariable UUID employeeId,
            @RequestParam(defaultValue = "2026") int year
    ) {
        List<LeaveBalanceDto> response = leaveService.getEmployeeBalances(employeeId, year);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/conflicts")
    @PreAuthorize("hasAnyRole('MANAGER', 'DRH')")
    @Operation(summary = "Détection des conflits de congés d'équipe", description = "Vérifie les chevauchements d'absences dans une même équipe sur une période")
    public ResponseEntity<ApiResponse<TeamLeaveConflictDto>> detectConflicts(
            @RequestParam UUID departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        TeamLeaveConflictDto response = leaveService.detectTeamConflicts(departmentId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

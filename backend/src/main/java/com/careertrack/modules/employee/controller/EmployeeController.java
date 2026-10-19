package com.careertrack.modules.employee.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.common.payload.PageResponse;
import com.careertrack.modules.employee.dto.*;
import com.careertrack.modules.employee.entity.EmployeeStatus;
import com.careertrack.modules.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employés", description = "Gestion des dossiers collaborateurs, contrats et fiches de poste")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER')")
    @Operation(summary = "Liste paginée des employés (DTO réduit)", description = "Récupère les employés avec pagination, recherche et filtres (DRH et Managers)")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeSummaryDto>>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        PageResponse<EmployeeSummaryDto> response = employeeService.getAllEmployees(
                page, size, search, departmentId, status, sortBy, sortDir
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@employeeSecurity.canAccessEmployee(#id)")
    @Operation(summary = "Détail complet d'un collaborateur", description = "Retourne le dossier modulaire d'un employé selon les autorisations du rôle")
    public ResponseEntity<ApiResponse<EmployeeDetailDto>> getEmployeeById(@PathVariable UUID id) {
        EmployeeDetailDto response = employeeService.getEmployeeDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Créer un employé", description = "Enregistre un nouveau collaborateur dans la plateforme")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> createEmployee(@Valid @RequestBody EmployeeCreateDto createDto) {
        EmployeeResponseDto response = employeeService.createEmployee(createDto);
        return new ResponseEntity<>(ApiResponse.success("Employé créé avec succès", response), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Mettre à jour un employé (DRH)", description = "Modifie l'ensemble du dossier employé avec contrôle de cycle hiérarchique")
    public ResponseEntity<ApiResponse<EmployeeDetailDto>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeUpdateDto updateDto
    ) {
        EmployeeDetailDto response = employeeService.updateEmployee(id, updateDto);
        return ResponseEntity.ok(ApiResponse.success("Dossier employé mis à jour", response));
    }

    @PatchMapping("/{id}/contact")
    @PreAuthorize("hasRole('DRH') or @employeeSecurity.canAccessEmployee(#id)")
    @Operation(summary = "Mise à jour des coordonnées personnelles", description = "Permet à l'employé ou au manager de modifier son propre téléphone et son adresse")
    public ResponseEntity<ApiResponse<EmployeeDetailDto>> updateSelfContact(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeSelfUpdateDto contactDto
    ) {
        EmployeeDetailDto response = employeeService.updateSelfContact(id, contactDto);
        return ResponseEntity.ok(ApiResponse.success("Coordonnées mises à jour avec succès", response));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Désactiver un employé", description = "Passe le statut à TERMINATED, clôture le contrat actif et désactive le compte utilisateur lié")
    public ResponseEntity<ApiResponse<Void>> deactivateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeDeactivateDto deactivateDto
    ) {
        employeeService.deactivateEmployee(id, deactivateDto);
        return ResponseEntity.ok(ApiResponse.success("Employé désactivé avec succès", null));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("@employeeSecurity.canAccessEmployee(#id)")
    @Operation(summary = "Historique des avancements", description = "Retourne l'historique des changements de poste et de salaire du collaborateur")
    public ResponseEntity<ApiResponse<EmployeeHistoryDto>> getEmployeeHistory(@PathVariable UUID id) {
        EmployeeHistoryDto response = employeeService.getEmployeeHistory(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Supprimer un employé", description = "Supprime un dossier collaborateur sans dépendances")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable UUID id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employé supprimé avec succès", null));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER')")
    @Operation(summary = "Employés par département", description = "Retourne les employés affectés à un département donné")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDto>>> getEmployeesByDepartment(@PathVariable UUID departmentId) {
        List<EmployeeResponseDto> response = employeeService.getEmployeesByDepartment(departmentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

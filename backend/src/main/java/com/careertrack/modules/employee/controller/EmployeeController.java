package com.careertrack.modules.employee.controller;

import com.careertrack.common.exception.BadRequestException;
import com.careertrack.common.payload.ApiResponse;
import com.careertrack.common.payload.PageResponse;
import com.careertrack.modules.employee.dto.EmployeeCreateDto;
import com.careertrack.modules.employee.dto.EmployeeResponseDto;
import com.careertrack.modules.employee.dto.EmployeeSummaryDto;
import com.careertrack.modules.employee.service.EmployeeService;
import com.careertrack.common.security.EmployeeSecurityService;
import com.careertrack.modules.employee.security.EmployeeListScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employés", description = "Gestion des dossiers collaborateurs, contrats et fiches de poste")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeSecurityService employeeSecurity;
    private final EmployeeListScope employeeListScope;

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("lastName", "firstName", "employeeNumber", "hireDate", "status");

    @GetMapping
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER')")
    @Operation(summary = "Liste paginée des employés", description = "Récupère les employés avec pagination et tri (DRH et Managers)")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeSummaryDto>>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID departmentId
    ) {
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Tri non autorisé : " + sortBy);
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        if (search != null && search.length() > 100) {
            throw new BadRequestException("Recherche trop longue (100 caractères maximum)");
        }

        PageResponse<EmployeeSummaryDto> response = employeeService.getAllEmployees(
                PageRequest.of(safePage, safeSize, Sort.by(sortBy).ascending()),
                search, departmentId, employeeListScope.managerScopeOrNull());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@employeeSecurity.canAccessEmployee(#id)")
    @Operation(summary = "Détails d'un employé", description = "Retourne le dossier complet d'un collaborateur par son ID")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> getEmployeeById(@PathVariable UUID id) {
        EmployeeResponseDto response = employeeService.getEmployeeById(id);
        if (!employeeSecurity.canSeeSensitiveData(id)) {
            response.setNationalId(null);
            response.setBirthDate(null);
            response.setPhone(null);
            response.setAddress(null);
        }
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
    @Operation(summary = "Mettre à jour un employé", description = "Modifie les informations administratives d'un employé")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeCreateDto updateDto
    ) {
        EmployeeResponseDto response = employeeService.updateEmployee(id, updateDto);
        return ResponseEntity.ok(ApiResponse.success("Dossier employé mis à jour", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Désactiver un employé", description = "Passe le dossier au statut TERMINATED sans le supprimer, pour conserver l'historique")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable UUID id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employé désactivé avec succès", null));
    }
}
package com.careertrack.modules.employee.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.common.payload.PageResponse;
import com.careertrack.modules.employee.dto.EmployeeCreateDto;
import com.careertrack.modules.employee.dto.EmployeeResponseDto;
import com.careertrack.modules.employee.service.EmployeeService;
import com.careertrack.common.security.EmployeeSecurityService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employés", description = "Gestion des dossiers collaborateurs, contrats et fiches de poste")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeSecurityService employeeSecurity;

    @GetMapping
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER')")
    @Operation(summary = "Liste paginée des employés", description = "Récupère les employés avec pagination et tri (DRH et Managers)")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponseDto>>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastName") String sortBy
    ) {
        PageResponse<EmployeeResponseDto> response = employeeService.getAllEmployees(
                PageRequest.of(page, size, Sort.by(sortBy).ascending()));
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
    @Operation(summary = "Supprimer un employé", description = "Supprime un dossier collaborateur")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable UUID id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employé supprimé avec succès", null));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER')")
    @Operation(summary = "Employés par département", description = "Retourne les employés affectés à un département donné (DRH et Managers)")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDto>>> getEmployeesByDepartment(@PathVariable UUID departmentId) {
        List<EmployeeResponseDto> response = employeeService.getEmployeesByDepartment(departmentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
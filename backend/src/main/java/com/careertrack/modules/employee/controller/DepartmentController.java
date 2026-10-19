package com.careertrack.modules.employee.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.employee.dto.DepartmentDto;
import com.careertrack.modules.employee.repository.DepartmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Départements", description = "Référentiel des départements de l'entreprise")
@SecurityRequirement(name = "BearerAuth")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Liste des départements", description = "Retourne la liste des départements pour les filtres et formulaires")
    public ResponseEntity<ApiResponse<List<DepartmentDto>>> getAllDepartments() {
        List<DepartmentDto> dtos = departmentRepository.findAll().stream()
                .map(d -> DepartmentDto.builder()
                        .id(d.getId())
                        .code(d.getCode())
                        .name(d.getName())
                        .description(d.getDescription())
                        .managerId(d.getManager() != null ? d.getManager().getId() : null)
                        .managerName(d.getManager() != null ? d.getManager().getFullName() : null)
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
}

package com.careertrack.modules.employee.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.employee.dto.ReferenceItemDto;
import com.careertrack.modules.employee.repository.DepartmentRepository;
import com.careertrack.modules.employee.repository.PositionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Référentiels RH", description = "Listes de départements et de postes pour les formulaires")
@SecurityRequirement(name = "BearerAuth")
public class ReferenceDataController {

    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;

    @GetMapping("/departments")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Liste des départements", description = "Identifiant et nom de chaque département, triés par nom")
    public ResponseEntity<ApiResponse<List<ReferenceItemDto>>> getDepartments() {
        List<ReferenceItemDto> items = departmentRepository.findAll(Sort.by("name")).stream()
                .map(d -> new ReferenceItemDto(d.getId(), d.getName(), null))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/positions")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Liste des postes", description = "Identifiant, intitulé et département de chaque poste, triés par intitulé")
    public ResponseEntity<ApiResponse<List<ReferenceItemDto>>> getPositions() {
        List<ReferenceItemDto> items = positionRepository.findAll(Sort.by("title")).stream()
                .map(p -> new ReferenceItemDto(
                        p.getId(),
                        p.getTitle(),
                        p.getDepartment() != null ? p.getDepartment().getId() : null))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(items));
    }
}
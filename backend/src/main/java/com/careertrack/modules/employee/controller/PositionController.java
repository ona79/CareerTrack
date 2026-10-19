package com.careertrack.modules.employee.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.employee.dto.PositionDto;
import com.careertrack.modules.employee.entity.Position;
import com.careertrack.modules.employee.repository.PositionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/positions")
@RequiredArgsConstructor
@Tag(name = "Postes", description = "Référentiel des postes et fiches de fonction")
@SecurityRequirement(name = "BearerAuth")
public class PositionController {

    private final PositionRepository positionRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Liste des postes", description = "Retourne la liste des postes, éventuellement filtrée par département")
    public ResponseEntity<ApiResponse<List<PositionDto>>> getPositions(
            @RequestParam(required = false) UUID departmentId
    ) {
        List<Position> positions;
        if (departmentId != null) {
            positions = positionRepository.findByDepartmentId(departmentId);
        } else {
            positions = positionRepository.findAll();
        }

        List<PositionDto> dtos = positions.stream()
                .map(p -> PositionDto.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .title(p.getTitle())
                        .departmentId(p.getDepartment() != null ? p.getDepartment().getId() : null)
                        .departmentName(p.getDepartment() != null ? p.getDepartment().getName() : null)
                        .minSalary(p.getMinSalary())
                        .maxSalary(p.getMaxSalary())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
}

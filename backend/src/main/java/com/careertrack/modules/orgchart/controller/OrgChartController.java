package com.careertrack.modules.orgchart.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.orgchart.dto.OrgNodeDto;
import com.careertrack.modules.orgchart.service.OrgChartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orgchart")
@RequiredArgsConstructor
@Tag(name = "Organigramme", description = "Visualisation hiérarchique de l'entreprise et relations managers/subordonnés")
@SecurityRequirement(name = "BearerAuth")
public class OrgChartController {

    private final OrgChartService orgChartService;

    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER', 'EMPLOYE', 'DIRECTION')")
    @Operation(summary = "Arbre complet de l'entreprise", description = "Retourne l'organigramme complet depuis la direction générale jusqu'aux collaborateurs")
    public ResponseEntity<ApiResponse<List<OrgNodeDto>>> getCompanyTree() {
        List<OrgNodeDto> tree = orgChartService.getCompanyOrgTree();
        return ResponseEntity.ok(ApiResponse.success(tree));
    }

    @GetMapping("/subordinates/{employeeId}")
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER', 'DIRECTION')")
    @Operation(summary = "Sous-arbre hiérarchique", description = "Retourne l'organigramme des subordonnés d'un manager spécifique")
    public ResponseEntity<ApiResponse<OrgNodeDto>> getSubtree(@PathVariable UUID employeeId) {
        OrgNodeDto subtree = orgChartService.getSubtreeForEmployee(employeeId);
        return ResponseEntity.ok(ApiResponse.success(subtree));
    }
}

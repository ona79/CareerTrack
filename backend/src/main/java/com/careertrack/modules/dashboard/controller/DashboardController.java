package com.careertrack.modules.dashboard.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.dashboard.dto.DashboardStatsDto;
import com.careertrack.modules.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Tableau de Bord RH", description = "KPIs, statistiques effectifs, pyramide des âges, paie et turnover")
@SecurityRequirement(name = "BearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('MANAGER', 'DRH', 'DIRECTION')")
    @Operation(summary = "Obtenir les statistiques RH et KPIs", description = "Retourne l'effectif, la pyramide des âges, le turnover, la masse salariale et la formation avec filtres")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getStats(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID positionId,
            @RequestParam(required = false) Integer periodYear,
            Authentication authentication
    ) {
        DashboardStatsDto stats = dashboardService.getDashboardStats(departmentId, positionId, periodYear, authentication);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

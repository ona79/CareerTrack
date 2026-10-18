package com.careertrack.modules.evaluation.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.evaluation.dto.EvaluationCampaignDto;
import com.careertrack.modules.evaluation.dto.EvaluationDto;
import com.careertrack.modules.evaluation.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
@Tag(name = "Évaluations & Performance", description = "Campagnes annuelles, objectifs SMART et entretiens N+1")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @GetMapping("/campaigns")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lister les campagnes d'évaluations", description = "Accessible à tous les collaborateurs connectés")
    public ResponseEntity<ApiResponse<List<EvaluationCampaignDto>>> getCampaigns() {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.getAllCampaigns()));
    }

    @PostMapping("/campaigns")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Lancer une nouvelle campagne annuelle d'évaluation", description = "Réservé à la DRH")
    public ResponseEntity<ApiResponse<EvaluationCampaignDto>> createCampaign(@RequestBody EvaluationCampaignDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Campagne créée", evaluationService.createCampaign(dto)));
    }

    @GetMapping("/campaigns/{campaignId}/evaluations")
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER')")
    @Operation(summary = "Lister les fiches d'évaluation d'une campagne", description = "Accessible aux managers et à la DRH")
    public ResponseEntity<ApiResponse<List<EvaluationDto>>> getEvaluationsByCampaign(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.getEvaluationsByCampaign(campaignId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@evaluationSecurity.canAccessEvaluation(#id)")
    @Operation(summary = "Consulter le détail d'une fiche d'évaluation et ses objectifs SMART", description = "Accessible à l'évaluateur, à l'évalué et à la DRH")
    public ResponseEntity<ApiResponse<EvaluationDto>> getEvaluationById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.getEvaluationById(id)));
    }
}

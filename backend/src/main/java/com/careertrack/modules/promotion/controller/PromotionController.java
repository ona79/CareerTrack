package com.careertrack.modules.promotion.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.promotion.dto.PromotionProposalDto;
import com.careertrack.modules.promotion.entity.PromotionStatus;
import com.careertrack.modules.promotion.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions & Carrières", description = "Propositions de promotion interne, revalorisations et simulations salariales")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER')")
    @Operation(summary = "Lister les propositions de promotion", description = "Accessible à la DRH et aux managers")
    public ResponseEntity<ApiResponse<List<PromotionProposalDto>>> getAllProposals() {
        return ResponseEntity.ok(ApiResponse.success(promotionService.getAllProposals()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER')")
    @Operation(summary = "Soumettre une proposition de promotion ou revalorisation", description = "Accessible au manager N+1 ou à la DRH")
    public ResponseEntity<ApiResponse<PromotionProposalDto>> createProposal(@RequestBody PromotionProposalDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Proposition soumise", promotionService.createProposal(dto)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Valider ou refuser une proposition de promotion", description = "Réservé à la DRH uniquement")
    public ResponseEntity<ApiResponse<PromotionProposalDto>> updateStatus(
            @PathVariable UUID id,
            @RequestParam PromotionStatus status,
            @RequestParam(required = false) String comment) {
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour", promotionService.updateStatus(id, status, comment)));
    }

    @PostMapping("/simulations")
    @PreAuthorize("hasAnyRole('DRH', 'DIRECTION')")
    @Operation(summary = "Simuler l'impact d'une revalorisation sur la masse salariale en FCFA", description = "Réservé à la DRH et à la DIRECTION")
    public ResponseEntity<ApiResponse<com.careertrack.modules.promotion.dto.SalarySimulationResultDto>> simulateSalaryImpact(
            @jakarta.validation.Valid @RequestBody com.careertrack.modules.promotion.dto.SalarySimulationRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Simulation calculée avec succès", promotionService.simulateSalaryImpact(dto)));
    }

    @GetMapping("/simulations")
    @PreAuthorize("hasAnyRole('DRH', 'DIRECTION')")
    @Operation(summary = "Lister l'historique des simulations de masse salariale", description = "Réservé à la DRH et à la DIRECTION")
    public ResponseEntity<ApiResponse<List<com.careertrack.modules.promotion.dto.SalarySimulationResultDto>>> getSalarySimulations() {
        return ResponseEntity.ok(ApiResponse.success(promotionService.getSalarySimulations()));
    }

    @GetMapping("/simulations/{id}")
    @PreAuthorize("hasAnyRole('DRH', 'DIRECTION')")
    @Operation(summary = "Détails d'une simulation de masse salariale", description = "Réservé à la DRH et à la DIRECTION")
    public ResponseEntity<ApiResponse<com.careertrack.modules.promotion.dto.SalarySimulationResultDto>> getSalarySimulation(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.getSalarySimulationById(id)));
    }
}

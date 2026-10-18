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
}

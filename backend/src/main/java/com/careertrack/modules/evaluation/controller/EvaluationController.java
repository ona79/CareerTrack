package com.careertrack.modules.evaluation.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.evaluation.dto.*;
import com.careertrack.modules.evaluation.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
@Tag(name = "Évaluations & Performance", description = "Campagnes annuelles, objectifs SMART, entretiens N+1 et bilan PDF")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @GetMapping("/campaigns")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lister les campagnes d'évaluations", description = "Accessible à tous les collaborateurs connectés")
    public ResponseEntity<ApiResponse<List<EvaluationCampaignDto>>> getCampaigns() {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.getAllCampaigns()));
    }

    @PostMapping("/campaigns")
    @PreAuthorize("hasAnyRole('DRH', 'ADMIN', 'DIRECTION')")
    @Operation(summary = "Lancer une nouvelle campagne annuelle d'évaluation", description = "Réservé à la DRH ou ADMIN")
    public ResponseEntity<ApiResponse<EvaluationCampaignDto>> createCampaign(@Valid @RequestBody EvaluationCampaignDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Campagne créée avec succès", evaluationService.createCampaign(dto)));
    }

    @PatchMapping("/campaigns/{campaignId}/close")
    @PreAuthorize("hasAnyRole('DRH', 'ADMIN')")
    @Operation(summary = "Clore une campagne d'évaluation", description = "Réservé à la DRH")
    public ResponseEntity<ApiResponse<EvaluationCampaignDto>> closeCampaign(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(ApiResponse.success("Campagne clôturée", evaluationService.closeCampaign(campaignId)));
    }

    @GetMapping("/campaigns/{campaignId}/evaluations")
    @PreAuthorize("hasAnyRole('DRH', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Lister les fiches d'évaluation d'une campagne", description = "Accessible aux managers et à la DRH")
    public ResponseEntity<ApiResponse<List<EvaluationDto>>> getEvaluationsByCampaign(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.getEvaluationsByCampaign(campaignId)));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consulter mes fiches d'évaluation annuelles", description = "L'employé ne voit que ses propres évaluations")
    public ResponseEntity<ApiResponse<List<EvaluationDto>>> getMyEvaluations(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.getMyEvaluations(principal.getId())));
    }

    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('MANAGER', 'DRH', 'ADMIN')")
    @Operation(summary = "Consulter les fiches d'évaluation de mon équipe", description = "Le manager voit les fiches de ses collaborateurs directs")
    public ResponseEntity<ApiResponse<List<EvaluationDto>>> getTeamEvaluations(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.getTeamEvaluations(principal.getId())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@evaluationSecurity.canAccessEvaluation(#id)")
    @Operation(summary = "Consulter le détail d'une fiche d'évaluation et ses objectifs SMART", description = "Accessible à l'évaluateur, à l'évalué et à la DRH")
    public ResponseEntity<ApiResponse<EvaluationDto>> getEvaluationById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.getEvaluationById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DRH', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Initialiser une fiche d'évaluation annuelle pour un employé", description = "Accessible aux managers et à la DRH")
    public ResponseEntity<ApiResponse<EvaluationDto>> createEvaluation(@Valid @RequestBody EvaluationCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Fiche d'évaluation initialisée", evaluationService.createEvaluation(dto)));
    }

    @PatchMapping("/{id}/self-eval")
    @PreAuthorize("@evaluationSecurity.canSelfEvaluate(#id)")
    @Operation(summary = "Soumettre l'auto-évaluation du collaborateur", description = "Réservé au salarié évalué")
    public ResponseEntity<ApiResponse<EvaluationDto>> submitSelfEvaluation(
            @PathVariable UUID id,
            @Valid @RequestBody EvaluationSelfSubmitDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success("Auto-évaluation enregistrée", evaluationService.submitSelfEvaluation(id, dto, principal.getId())));
    }

    @PatchMapping("/{id}/manager-eval")
    @PreAuthorize("@evaluationSecurity.canManagerEvaluate(#id)")
    @Operation(summary = "Soumettre l'évaluation managériale N+1 avec calcul du score pondéré", description = "Réservé au manager évaluateur ou à la DRH")
    public ResponseEntity<ApiResponse<EvaluationDto>> submitManagerEvaluation(
            @PathVariable UUID id,
            @Valid @RequestBody EvaluationManagerSubmitDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success("Évaluation managériale enregistrée et score pondéré calculé", evaluationService.submitManagerEvaluation(id, dto, principal.getId())));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("@evaluationSecurity.canComplete(#id)")
    @Operation(summary = "Valider définitivement et clore l'évaluation annuelle", description = "Clôture de l'entretien et génération de la synthèse")
    public ResponseEntity<ApiResponse<EvaluationDto>> completeEvaluation(
            @PathVariable UUID id,
            @RequestBody(required = false) EvaluationCompleteDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.success("Évaluation validée et clôturée", evaluationService.completeEvaluation(id, dto)));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("@evaluationSecurity.canDownloadPdf(#id)")
    @Operation(summary = "Télécharger le bilan d'entretien annuel signable en PDF", description = "Généré via iText avec synthèse, objectifs SMART et encadrés de signature")
    public ResponseEntity<byte[]> downloadEvaluationPdf(@PathVariable UUID id) {
        byte[] pdfBytes = evaluationService.generateEvaluationPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"entretien_annuel_" + id + ".pdf\"")
                .body(pdfBytes);
    }
}

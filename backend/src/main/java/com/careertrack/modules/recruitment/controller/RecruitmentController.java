package com.careertrack.modules.recruitment.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.recruitment.dto.JobApplicationCreateDto;
import com.careertrack.modules.recruitment.dto.JobApplicationDto;
import com.careertrack.modules.recruitment.dto.JobOfferCreateDto;
import com.careertrack.modules.recruitment.dto.JobOfferDto;
import com.careertrack.modules.recruitment.entity.ApplicationStatus;
import com.careertrack.modules.recruitment.entity.JobOffer;
import com.careertrack.modules.recruitment.service.RecruitmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recruitment")
@RequiredArgsConstructor
@Tag(name = "Recrutement", description = "Pipeline Kanban des candidatures et offres d'emploi")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER', 'DIRECTION', 'ADMIN')")
    @Operation(summary = "Lister toutes les candidatures pour le tableau Kanban", description = "Accessible à la DRH et aux Managers")
    public ResponseEntity<ApiResponse<List<JobApplicationDto>>> getAllApplications() {
        return ResponseEntity.ok(ApiResponse.success(recruitmentService.getAllApplications()));
    }

    @PostMapping("/applications")
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER', 'DIRECTION', 'ADMIN')")
    @Operation(summary = "Enregistrer une nouvelle candidature pour une offre", description = "Accessible à la DRH et aux Managers")
    public ResponseEntity<ApiResponse<JobApplicationDto>> createApplication(
            @jakarta.validation.Valid @RequestBody JobApplicationCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Candidature enregistrée avec succès", recruitmentService.createApplication(dto)));
    }

    @PatchMapping("/applications/{id}/status")
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER', 'DIRECTION', 'ADMIN')")
    @Operation(summary = "Déplacer une candidature (Kanban glisser-déposer)", description = "Met à jour l'étape du pipeline et déclenche une notification e-mail")
    public ResponseEntity<ApiResponse<JobApplicationDto>> updateStatus(
            @PathVariable UUID id,
            @RequestParam ApplicationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                "Statut de la candidature mis à jour avec succès",
                recruitmentService.updateApplicationStatus(id, status)
        ));
    }

    @GetMapping("/offers")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lister les offres d'emploi publiées", description = "Accessible aux utilisateurs authentifiés")
    public ResponseEntity<ApiResponse<List<JobOfferDto>>> getOffers() {
        return ResponseEntity.ok(ApiResponse.success(recruitmentService.getAllOffers()));
    }

    @PostMapping("/offers")
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER', 'DIRECTION', 'ADMIN')")
    @Operation(summary = "Publier une nouvelle offre d'emploi", description = "Accessible à la DRH et aux Managers")
    public ResponseEntity<ApiResponse<JobOfferDto>> createOffer(
            @jakarta.validation.Valid @RequestBody JobOfferCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Offre d'emploi publiée avec succès", recruitmentService.createJobOffer(dto)));
    }

    @GetMapping("/offers/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Détail d'une offre d'emploi", description = "Accessible aux utilisateurs authentifiés")
    public ResponseEntity<ApiResponse<JobOfferDto>> getOfferById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(recruitmentService.getOfferById(id)));
    }
}

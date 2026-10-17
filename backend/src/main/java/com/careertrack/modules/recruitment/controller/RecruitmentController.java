package com.careertrack.modules.recruitment.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.recruitment.dto.JobApplicationDto;
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
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER')")
    @Operation(summary = "Lister toutes les candidatures pour le tableau Kanban", description = "Accessible à la DRH et aux Managers")
    public ResponseEntity<ApiResponse<List<JobApplicationDto>>> getAllApplications() {
        return ResponseEntity.ok(ApiResponse.success(recruitmentService.getAllApplications()));
    }

    @PatchMapping("/applications/{id}/status")
    @PreAuthorize("hasAnyRole('DRH', 'MANAGER')")
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
    @Operation(summary = "Lister les offres d'emploi publiées", description = "Accessible publiquement ou en interne")
    public ResponseEntity<ApiResponse<List<JobOffer>>> getOffers() {
        return ResponseEntity.ok(ApiResponse.success(recruitmentService.getPublishedOffers()));
    }
}

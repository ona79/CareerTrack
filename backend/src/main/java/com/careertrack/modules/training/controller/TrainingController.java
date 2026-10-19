package com.careertrack.modules.training.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.training.dto.*;
import com.careertrack.modules.training.entity.TrainingSessionStatus;
import com.careertrack.modules.training.service.TrainingService;
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
@RequestMapping("/api/v1/trainings")
@RequiredArgsConstructor
@Tag(name = "Formations & Compétences", description = "Catalogue, sessions, inscriptions et attestations de formation PDF")
public class TrainingController {

    private final TrainingService trainingService;

    // ─── CATALOGUE ────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consulter le catalogue de formation", description = "Accessible à l'ensemble des collaborateurs")
    public ResponseEntity<ApiResponse<List<TrainingDto>>> getAllTrainings() {
        return ResponseEntity.ok(ApiResponse.success(trainingService.getAllTrainings()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtenir le détail d'un module de formation", description = "Accessible à l'ensemble des collaborateurs")
    public ResponseEntity<ApiResponse<TrainingDto>> getTrainingById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.getTrainingById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DRH', 'ADMIN')")
    @Operation(summary = "Ajouter un nouveau module de formation au catalogue", description = "Réservé à la DRH ou ADMIN")
    public ResponseEntity<ApiResponse<TrainingDto>> createTraining(@Valid @RequestBody TrainingDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Module de formation créé avec succès", trainingService.createTraining(dto)));
    }

    // ─── SESSIONS DE FORMATION ────────────────────────────────────────────────

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lister l'ensemble des sessions de formation", description = "Accessible à tous les collaborateurs")
    public ResponseEntity<ApiResponse<List<TrainingSessionDto>>> getAllSessions() {
        return ResponseEntity.ok(ApiResponse.success(trainingService.getAllSessions()));
    }

    @GetMapping("/{trainingId}/sessions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lister les sessions associées à un module de formation", description = "Accessible à tous les collaborateurs")
    public ResponseEntity<ApiResponse<List<TrainingSessionDto>>> getSessionsByTraining(@PathVariable UUID trainingId) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.getSessionsByTraining(trainingId)));
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consulter le détail d'une session de formation", description = "Accessible à tous les collaborateurs")
    public ResponseEntity<ApiResponse<TrainingSessionDto>> getSessionById(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.getSessionById(sessionId)));
    }

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('DRH', 'ADMIN')")
    @Operation(summary = "Planifier une nouvelle session de formation", description = "Réservé à la DRH ou ADMIN")
    public ResponseEntity<ApiResponse<TrainingSessionDto>> createSession(@Valid @RequestBody TrainingSessionCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Session planifiée avec succès", trainingService.createSession(dto)));
    }

    @PatchMapping("/sessions/{sessionId}/status")
    @PreAuthorize("hasAnyRole('DRH', 'ADMIN')")
    @Operation(summary = "Modifier le statut d'une session de formation", description = "Réservé à la DRH")
    public ResponseEntity<ApiResponse<TrainingSessionDto>> updateSessionStatus(
            @PathVariable UUID sessionId,
            @RequestParam TrainingSessionStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.success("Statut de session mis à jour", trainingService.updateSessionStatus(sessionId, status)));
    }

    // ─── INSCRIPTIONS (ENROLLMENTS) ───────────────────────────────────────────

    @PostMapping("/sessions/enroll")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "S'inscrire ou inscrire un collaborateur à une session", description = "Vérification automatique de la capacité maximale")
    public ResponseEntity<ApiResponse<TrainingEnrollmentDto>> enrollEmployee(
            @Valid @RequestBody EnrollmentRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success("Inscription enregistrée avec succès", trainingService.enrollEmployee(dto, principal.getId())));
    }

    @GetMapping("/my-enrollments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consulter mes inscriptions aux formations", description = "L'employé consulte son parcours personnel de formation")
    public ResponseEntity<ApiResponse<List<TrainingEnrollmentDto>>> getMyEnrollments(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.getMyEnrollments(principal.getId())));
    }

    @GetMapping("/sessions/{sessionId}/enrollments")
    @PreAuthorize("hasAnyRole('DRH', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Lister les inscrits à une session", description = "Réservé aux managers et à la DRH")
    public ResponseEntity<ApiResponse<List<TrainingEnrollmentDto>>> getEnrollmentsBySession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.getEnrollmentsBySession(sessionId)));
    }

    @PatchMapping("/enrollments/{enrollmentId}/status")
    @PreAuthorize("hasAnyRole('DRH', 'ADMIN')")
    @Operation(summary = "Valider la participation ou la complétion d'une formation", description = "Réservé à la DRH")
    public ResponseEntity<ApiResponse<TrainingEnrollmentDto>> updateEnrollmentStatus(
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody EnrollmentStatusUpdateDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.success("Statut d'inscription mis à jour", trainingService.updateEnrollmentStatus(enrollmentId, dto)));
    }

    // ─── ATTESTATION DE FORMATION PDF ─────────────────────────────────────────

    @GetMapping("/enrollments/{enrollmentId}/certificate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Télécharger l'attestation officielle de formation en PDF", description = "Générée automatiquement avec iText pour les formations suivies")
    public ResponseEntity<byte[]> downloadCertificatePdf(@PathVariable UUID enrollmentId) {
        byte[] pdfBytes = trainingService.generateCertificatePdf(enrollmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attestation_formation_" + enrollmentId + ".pdf\"")
                .body(pdfBytes);
    }
}

package com.careertrack.modules.training.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.training.dto.TrainingDto;
import com.careertrack.modules.training.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trainings")
@RequiredArgsConstructor
@Tag(name = "Formations", description = "Catalogue des compétences et sessions de formation continue")
public class TrainingController {

    private final TrainingService trainingService;

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
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Ajouter un nouveau module de formation au catalogue", description = "Réservé à la DRH")
    public ResponseEntity<ApiResponse<TrainingDto>> createTraining(@RequestBody TrainingDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Module de formation créé avec succès", trainingService.createTraining(dto)));
    }
}

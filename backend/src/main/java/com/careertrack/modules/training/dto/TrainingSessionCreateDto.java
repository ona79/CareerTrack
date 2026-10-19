package com.careertrack.modules.training.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingSessionCreateDto {

    @NotNull(message = "L'identifiant de la formation est obligatoire")
    private UUID trainingId;

    @NotBlank(message = "Le code de session est obligatoire")
    private String sessionCode;

    @NotNull(message = "La date de début est obligatoire")
    private Instant startDate;

    @NotNull(message = "La date de fin est obligatoire")
    private Instant endDate;

    private String location;

    @Builder.Default
    private int maxParticipants = 15;
}

package com.careertrack.modules.training.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRequestDto {

    @NotNull(message = "L'identifiant de la session est obligatoire")
    private UUID sessionId;

    private UUID employeeId; // Optionnel : si absent, c'est l'employé connecté
}

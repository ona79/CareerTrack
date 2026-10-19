package com.careertrack.modules.promotion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalarySimulationRequestDto {
    @NotBlank(message = "Le titre de la simulation est obligatoire")
    private String title;

    private UUID departmentId;

    @NotNull(message = "Le taux d'augmentation est obligatoire")
    @DecimalMin(value = "0.0", message = "Le taux d'augmentation ne peut pas être négatif")
    @DecimalMax(value = "100.0", message = "Le taux d'augmentation ne peut pas dépasser 100%")
    private BigDecimal averageIncreasePercentage;
}

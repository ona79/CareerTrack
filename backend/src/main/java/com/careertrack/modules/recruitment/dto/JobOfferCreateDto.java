package com.careertrack.modules.recruitment.dto;

import com.careertrack.modules.employee.entity.ContractType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobOfferCreateDto {
    private String code;

    @NotBlank(message = "L'intitulé du poste est obligatoire")
    private String title;

    private UUID departmentId;
    private UUID positionId;

    @NotNull(message = "Le type de contrat est obligatoire")
    private ContractType contractType;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    private String requirements;

    @Builder.Default
    private int openPositions = 1;

    private LocalDate closingDate;
}

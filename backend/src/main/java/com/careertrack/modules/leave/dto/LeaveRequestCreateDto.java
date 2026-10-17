package com.careertrack.modules.leave.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestCreateDto {

    @NotNull(message = "L'employé est obligatoire")
    private UUID employeeId;

    @NotNull(message = "Le type de congé est obligatoire")
    private UUID leaveTypeId;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate endDate;

    @NotNull(message = "Le nombre de jours est obligatoire")
    private BigDecimal requestedDays;

    private String reason;
}

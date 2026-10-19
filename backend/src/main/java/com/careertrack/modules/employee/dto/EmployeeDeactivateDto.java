package com.careertrack.modules.employee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDeactivateDto {

    @NotBlank(message = "Le motif de désactivation est obligatoire")
    private String reason;

    private LocalDate departureDate;
}

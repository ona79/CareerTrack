package com.careertrack.modules.training.dto;

import com.careertrack.modules.training.entity.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentStatusUpdateDto {

    @NotNull(message = "Le statut est obligatoire")
    private EnrollmentStatus status;

    private BigDecimal score;

    private String feedback;
}

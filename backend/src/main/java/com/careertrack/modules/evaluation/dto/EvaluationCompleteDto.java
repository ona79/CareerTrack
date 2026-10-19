package com.careertrack.modules.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCompleteDto {
    private String finalFeedback;
    private BigDecimal finalScore;
}

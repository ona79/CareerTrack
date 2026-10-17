package com.careertrack.modules.evaluation.dto;

import com.careertrack.modules.evaluation.entity.CampaignStatus;
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
public class EvaluationCampaignDto {
    private UUID id;
    private String title;
    private int campaignYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private CampaignStatus status;
}

package com.careertrack.modules.evaluation.service;

import com.careertrack.modules.evaluation.dto.EvaluationCampaignDto;
import com.careertrack.modules.evaluation.dto.EvaluationDto;

import java.util.List;
import java.util.UUID;

public interface EvaluationService {
    List<EvaluationCampaignDto> getAllCampaigns();
    EvaluationCampaignDto createCampaign(EvaluationCampaignDto dto);
    List<EvaluationDto> getEvaluationsByCampaign(UUID campaignId);
    List<EvaluationDto> getEvaluationsByEmployee(UUID employeeId);
    EvaluationDto getEvaluationById(UUID evaluationId);
}

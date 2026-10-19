package com.careertrack.modules.evaluation.service;

import com.careertrack.modules.evaluation.dto.*;

import java.util.List;
import java.util.UUID;

public interface EvaluationService {
    List<EvaluationCampaignDto> getAllCampaigns();
    EvaluationCampaignDto createCampaign(EvaluationCampaignDto dto);
    EvaluationCampaignDto closeCampaign(UUID campaignId);

    List<EvaluationDto> getEvaluationsByCampaign(UUID campaignId);
    List<EvaluationDto> getEvaluationsByEmployee(UUID employeeId);
    List<EvaluationDto> getMyEvaluations(UUID currentUserId);
    List<EvaluationDto> getTeamEvaluations(UUID currentManagerUserId);
    EvaluationDto getEvaluationById(UUID evaluationId);

    EvaluationDto createEvaluation(EvaluationCreateDto dto);
    EvaluationDto submitSelfEvaluation(UUID evaluationId, EvaluationSelfSubmitDto dto, UUID currentUserId);
    EvaluationDto submitManagerEvaluation(UUID evaluationId, EvaluationManagerSubmitDto dto, UUID currentManagerUserId);
    EvaluationDto completeEvaluation(UUID evaluationId, EvaluationCompleteDto dto);

    byte[] generateEvaluationPdf(UUID evaluationId);
}

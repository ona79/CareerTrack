package com.careertrack.modules.evaluation.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.evaluation.dto.EvaluationCampaignDto;
import com.careertrack.modules.evaluation.dto.EvaluationDto;
import com.careertrack.modules.evaluation.entity.Evaluation;
import com.careertrack.modules.evaluation.entity.EvaluationCampaign;
import com.careertrack.modules.evaluation.entity.Objective;
import com.careertrack.modules.evaluation.repository.EvaluationCampaignRepository;
import com.careertrack.modules.evaluation.repository.EvaluationRepository;
import com.careertrack.modules.evaluation.repository.ObjectiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationCampaignRepository campaignRepository;
    private final EvaluationRepository evaluationRepository;
    private final ObjectiveRepository objectiveRepository;

    public EvaluationServiceImpl(
            EvaluationCampaignRepository campaignRepository,
            EvaluationRepository evaluationRepository,
            ObjectiveRepository objectiveRepository) {
        this.campaignRepository = campaignRepository;
        this.evaluationRepository = evaluationRepository;
        this.objectiveRepository = objectiveRepository;
    }

    @Override
    public List<EvaluationCampaignDto> getAllCampaigns() {
        return campaignRepository.findAll().stream()
                .map(this::mapCampaignToDto)
                .toList();
    }

    @Override
    @Transactional
    public EvaluationCampaignDto createCampaign(EvaluationCampaignDto dto) {
        EvaluationCampaign campaign = EvaluationCampaign.builder()
                .title(dto.getTitle())
                .campaignYear(dto.getCampaignYear())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(dto.getStatus())
                .build();
        return mapCampaignToDto(campaignRepository.save(campaign));
    }

    @Override
    public List<EvaluationDto> getEvaluationsByCampaign(UUID campaignId) {
        return evaluationRepository.findByCampaignId(campaignId).stream()
                .map(this::mapEvaluationToDto)
                .toList();
    }

    @Override
    public List<EvaluationDto> getEvaluationsByEmployee(UUID employeeId) {
        return evaluationRepository.findByEmployeeId(employeeId).stream()
                .map(this::mapEvaluationToDto)
                .toList();
    }

    @Override
    public EvaluationDto getEvaluationById(UUID evaluationId) {
        Evaluation eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche d'évaluation introuvable : " + evaluationId));
        return mapEvaluationToDto(eval);
    }

    private EvaluationCampaignDto mapCampaignToDto(EvaluationCampaign c) {
        return EvaluationCampaignDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .campaignYear(c.getCampaignYear())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .status(c.getStatus())
                .build();
    }

    private EvaluationDto mapEvaluationToDto(Evaluation e) {
        List<Objective> objectives = objectiveRepository.findByEvaluationId(e.getId());
        List<EvaluationDto.SmartObjectiveDto> objDtos = objectives.stream()
                .map(o -> EvaluationDto.SmartObjectiveDto.builder()
                        .id(o.getId())
                        .title(o.getTitle())
                        .description(o.getDescription())
                        .category(o.getCategory())
                        .weight(o.getWeight())
                        .targetDate(o.getTargetDate())
                        .achievementRate(o.getAchievementRate())
                        .status(o.getStatus())
                        .build())
                .toList();

        return EvaluationDto.builder()
                .id(e.getId())
                .campaignId(e.getCampaign() != null ? e.getCampaign().getId() : null)
                .campaignTitle(e.getCampaign() != null ? e.getCampaign().getTitle() : null)
                .employeeId(e.getEmployee() != null ? e.getEmployee().getId() : null)
                .employeeFullName(e.getEmployee() != null ? e.getEmployee().getFullName() : null)
                .evaluatorId(e.getEvaluator() != null ? e.getEvaluator().getId() : null)
                .evaluatorFullName(e.getEvaluator() != null ? e.getEvaluator().getFullName() : null)
                .status(e.getStatus())
                .selfScore(e.getSelfScore())
                .managerScore(e.getManagerScore())
                .finalScore(e.getFinalScore())
                .feedback(e.getFeedback())
                .summaryPdfUrl(e.getSummaryPdfUrl())
                .interviewDate(e.getInterviewDate())
                .objectives(objDtos)
                .build();
    }
}

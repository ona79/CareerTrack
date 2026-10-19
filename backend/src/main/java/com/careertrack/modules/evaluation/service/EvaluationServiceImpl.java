package com.careertrack.modules.evaluation.service;

import com.careertrack.common.exception.BadRequestException;
import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.common.pdf.PdfGeneratorService;
import com.careertrack.common.pdf.template.AnnualEvaluationPdfData;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.evaluation.dto.*;
import com.careertrack.modules.evaluation.entity.*;
import com.careertrack.modules.evaluation.repository.EvaluationCampaignRepository;
import com.careertrack.modules.evaluation.repository.EvaluationRepository;
import com.careertrack.modules.evaluation.repository.ObjectiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationCampaignRepository campaignRepository;
    private final EvaluationRepository evaluationRepository;
    private final ObjectiveRepository objectiveRepository;
    private final EmployeeRepository employeeRepository;
    private final EvaluationScoreCalculator scoreCalculator;
    private final PdfGeneratorService pdfGeneratorService;

    @Override
    public List<EvaluationCampaignDto> getAllCampaigns() {
        return campaignRepository.findAll().stream()
                .map(this::mapCampaignToDto)
                .toList();
    }

    @Override
    @Transactional
    public EvaluationCampaignDto createCampaign(EvaluationCampaignDto dto) {
        int year = dto.getCampaignYear() > 0 ? dto.getCampaignYear() : LocalDate.now().getYear();
        LocalDate startDate = dto.getStartDate() != null ? dto.getStartDate() : LocalDate.of(year, 1, 1);
        LocalDate endDate = dto.getEndDate() != null ? dto.getEndDate() : LocalDate.of(year, 12, 31);
        String title = (dto.getTitle() != null && !dto.getTitle().isBlank())
                ? dto.getTitle()
                : "Campagne Annuelle d'Évaluation " + year;

        EvaluationCampaign campaign = EvaluationCampaign.builder()
                .title(title)
                .campaignYear(year)
                .startDate(startDate)
                .endDate(endDate)
                .status(dto.getStatus() != null ? dto.getStatus() : CampaignStatus.IN_PROGRESS)
                .build();
        EvaluationCampaign savedCampaign = campaignRepository.save(campaign);

        // Auto-génération des fiches d'évaluation pour tous les collaborateurs actifs
        List<Employee> activeEmployees = employeeRepository.findAllActiveWithDetails();
        for (Employee emp : activeEmployees) {
            Optional<Evaluation> existing = evaluationRepository.findByCampaignIdAndEmployeeId(savedCampaign.getId(), emp.getId());
            if (existing.isEmpty()) {
                Evaluation eval = Evaluation.builder()
                        .campaign(savedCampaign)
                        .employee(emp)
                        .evaluator(emp.getManager())
                        .status(EvaluationStatus.DRAFT)
                        .build();
                Evaluation savedEval = evaluationRepository.save(eval);

                Objective obj1 = Objective.builder()
                        .evaluation(savedEval)
                        .title("Objectifs de performance opérationnelle et livrables " + year)
                        .category(ObjectiveCategory.OPERATIONAL)
                        .weight(BigDecimal.valueOf(60))
                        .description("Taux de respect des engagements et qualité des livrables")
                        .achievementRate(BigDecimal.valueOf(100))
                        .status("IN_PROGRESS")
                        .build();
                objectiveRepository.save(obj1);

                Objective obj2 = Objective.builder()
                        .evaluation(savedEval)
                        .title("Développement des compétences et esprit d'équipe")
                        .category(ObjectiveCategory.LEARNING)
                        .weight(BigDecimal.valueOf(40))
                        .description("Participation active aux revues et acquisition de nouvelles compétences")
                        .achievementRate(BigDecimal.valueOf(100))
                        .status("IN_PROGRESS")
                        .build();
                objectiveRepository.save(obj2);
            }
        }

        return mapCampaignToDto(savedCampaign);
    }

    @Override
    @Transactional
    public EvaluationCampaignDto closeCampaign(UUID campaignId) {
        EvaluationCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne d'évaluation introuvable : " + campaignId));
        campaign.setStatus(CampaignStatus.CLOSED);
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
    public List<EvaluationDto> getMyEvaluations(UUID currentUserId) {
        Optional<Employee> empOpt = employeeRepository.findByUserId(currentUserId);
        if (empOpt.isEmpty()) {
            return Collections.emptyList();
        }
        return evaluationRepository.findByEmployeeId(empOpt.get().getId()).stream()
                .map(this::mapEvaluationToDto)
                .toList();
    }

    @Override
    public List<EvaluationDto> getTeamEvaluations(UUID currentManagerUserId) {
        Optional<Employee> mgrOpt = employeeRepository.findByUserId(currentManagerUserId);
        if (mgrOpt.isEmpty()) {
            return Collections.emptyList();
        }
        return evaluationRepository.findTeamEvaluations(mgrOpt.get().getId()).stream()
                .map(this::mapEvaluationToDto)
                .toList();
    }

    @Override
    public EvaluationDto getEvaluationById(UUID evaluationId) {
        Evaluation eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche d'évaluation introuvable : " + evaluationId));
        return mapEvaluationToDto(eval);
    }

    @Override
    @Transactional
    public EvaluationDto createEvaluation(EvaluationCreateDto dto) {
        EvaluationCampaign campaign = campaignRepository.findById(dto.getCampaignId())
                .orElseThrow(() -> new ResourceNotFoundException("Campagne introuvable : " + dto.getCampaignId()));

        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + dto.getEmployeeId()));

        Employee evaluator = null;
        if (dto.getEvaluatorId() != null) {
            evaluator = employeeRepository.findById(dto.getEvaluatorId()).orElse(null);
        } else if (employee.getManager() != null) {
            evaluator = employee.getManager();
        }

        Evaluation evaluation = Evaluation.builder()
                .campaign(campaign)
                .employee(employee)
                .evaluator(evaluator)
                .status(EvaluationStatus.DRAFT)
                .interviewDate(dto.getInterviewDate())
                .selfScore(BigDecimal.ZERO)
                .managerScore(BigDecimal.ZERO)
                .finalScore(BigDecimal.ZERO)
                .build();

        Evaluation saved = evaluationRepository.save(evaluation);

        if (dto.getObjectives() != null && !dto.getObjectives().isEmpty()) {
            for (EvaluationCreateDto.ObjectiveInputDto objDto : dto.getObjectives()) {
                Objective obj = Objective.builder()
                        .evaluation(saved)
                        .title(objDto.getTitle())
                        .description(objDto.getDescription())
                        .category(objDto.getCategory() != null ? objDto.getCategory() : ObjectiveCategory.OPERATIONAL)
                        .weight(objDto.getWeight() != null ? objDto.getWeight() : BigDecimal.valueOf(10))
                        .targetDate(objDto.getTargetDate())
                        .achievementRate(objDto.getAchievementRate() != null ? objDto.getAchievementRate() : BigDecimal.ZERO)
                        .status("IN_PROGRESS")
                        .build();
                saved.getObjectives().add(obj);
            }
            saved = evaluationRepository.save(saved);
        }

        return mapEvaluationToDto(saved);
    }

    @Override
    @Transactional
    public EvaluationDto submitSelfEvaluation(UUID evaluationId, EvaluationSelfSubmitDto dto, UUID currentUserId) {
        Evaluation eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche d'évaluation introuvable : " + evaluationId));

        if (dto.getSelfScore() != null) {
            eval.setSelfScore(dto.getSelfScore());
        }

        if (dto.getSelfFeedback() != null && !dto.getSelfFeedback().isBlank()) {
            String fb = eval.getFeedback();
            String prefix = "[Auto-évaluation Salarié] : " + dto.getSelfFeedback();
            eval.setFeedback(fb != null && !fb.isBlank() ? fb + "\n\n" + prefix : prefix);
        }

        if (dto.getObjectiveRates() != null && !dto.getObjectiveRates().isEmpty()) {
            Map<UUID, Objective> objMap = new HashMap<>();
            for (Objective o : eval.getObjectives()) {
                objMap.put(o.getId(), o);
            }
            for (EvaluationSelfSubmitDto.ObjectiveRateDto r : dto.getObjectiveRates()) {
                Objective o = objMap.get(r.getObjectiveId());
                if (o != null && r.getSelfRate() != null) {
                    o.setAchievementRate(r.getSelfRate());
                }
            }
        }

        eval.setStatus(EvaluationStatus.SELF_SUBMITTED);
        return mapEvaluationToDto(evaluationRepository.save(eval));
    }

    @Override
    @Transactional
    public EvaluationDto submitManagerEvaluation(UUID evaluationId, EvaluationManagerSubmitDto dto, UUID currentManagerUserId) {
        Evaluation eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche d'évaluation introuvable : " + evaluationId));

        if (dto.getManagerScore() != null) {
            eval.setManagerScore(dto.getManagerScore());
        }
        if (dto.getInterviewDate() != null) {
            eval.setInterviewDate(dto.getInterviewDate());
        }

        if (dto.getManagerFeedback() != null && !dto.getManagerFeedback().isBlank()) {
            String fb = eval.getFeedback();
            String prefix = "[Évaluation Manager N+1] : " + dto.getManagerFeedback();
            eval.setFeedback(fb != null && !fb.isBlank() ? fb + "\n\n" + prefix : prefix);
        }

        if (dto.getObjectiveRates() != null && !dto.getObjectiveRates().isEmpty()) {
            Map<UUID, Objective> objMap = new HashMap<>();
            for (Objective o : eval.getObjectives()) {
                objMap.put(o.getId(), o);
            }
            for (EvaluationManagerSubmitDto.ManagerObjectiveRateDto r : dto.getObjectiveRates()) {
                Objective o = objMap.get(r.getObjectiveId());
                if (o != null) {
                    if (r.getAchievementRate() != null) {
                        o.setAchievementRate(r.getAchievementRate());
                    }
                    if (r.getStatus() != null) {
                        o.setStatus(r.getStatus());
                    }
                }
            }
        }

        // Calcul automatique du score pondéré sur 5.0
        BigDecimal calculatedFinalScore = scoreCalculator.calculateWeightedScore(eval.getObjectives());
        eval.setFinalScore(calculatedFinalScore);

        eval.setStatus(EvaluationStatus.MANAGER_EVALUATED);
        return mapEvaluationToDto(evaluationRepository.save(eval));
    }

    @Override
    @Transactional
    public EvaluationDto completeEvaluation(UUID evaluationId, EvaluationCompleteDto dto) {
        Evaluation eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche d'évaluation introuvable : " + evaluationId));

        if (dto != null) {
            if (dto.getFinalScore() != null) {
                eval.setFinalScore(dto.getFinalScore());
            } else if (eval.getFinalScore() == null || eval.getFinalScore().compareTo(BigDecimal.ZERO) == 0) {
                eval.setFinalScore(scoreCalculator.calculateWeightedScore(eval.getObjectives()));
            }
            if (dto.getFinalFeedback() != null && !dto.getFinalFeedback().isBlank()) {
                String fb = eval.getFeedback();
                String prefix = "[Validation Finale] : " + dto.getFinalFeedback();
                eval.setFeedback(fb != null && !fb.isBlank() ? fb + "\n\n" + prefix : prefix);
            }
        }

        eval.setStatus(EvaluationStatus.COMPLETED);
        eval.setSummaryPdfUrl("/api/v1/evaluations/" + eval.getId() + "/pdf");

        return mapEvaluationToDto(evaluationRepository.save(eval));
    }

    @Override
    public byte[] generateEvaluationPdf(UUID evaluationId) {
        Evaluation eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche d'évaluation introuvable : " + evaluationId));

        Employee employee = eval.getEmployee();
        if (employee != null) {
            employee = employeeRepository.findById(employee.getId()).orElse(employee);
        }

        Employee evaluator = eval.getEvaluator() != null ? eval.getEvaluator() : (employee != null ? employee.getManager() : null);
        if (evaluator != null) {
            evaluator = employeeRepository.findById(evaluator.getId()).orElse(evaluator);
        }

        List<AnnualEvaluationPdfData.ObjectiveItem> objectiveItems = eval.getObjectives().stream()
                .map(o -> new AnnualEvaluationPdfData.ObjectiveItem(
                        o.getTitle(),
                        o.getDescription(),
                        o.getWeight() != null ? o.getWeight().intValue() : 0,
                        o.getAchievementRate() != null ? o.getAchievementRate().intValue() : 0,
                        o.getCategory() != null ? o.getCategory().name() : "KPI"
                ))
                .toList();

        BigDecimal displayScore = eval.getFinalScore();
        if (displayScore == null && eval.getManagerScore() != null) {
            displayScore = eval.getManagerScore();
        } else if (displayScore == null && eval.getSelfScore() != null) {
            displayScore = eval.getSelfScore();
        }

        AnnualEvaluationPdfData pdfData = new AnnualEvaluationPdfData(
                eval.getCampaign() != null ? eval.getCampaign().getTitle() : "Campagne Annuelle",
                employee != null ? employee.getFullName() : "N/A",
                employee != null ? employee.getEmployeeNumber() : "N/A",
                employee != null && employee.getPosition() != null ? employee.getPosition().getTitle() : "N/A",
                employee != null && employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A",
                evaluator != null ? evaluator.getFullName() : (employee != null && employee.getManager() != null ? employee.getManager().getFullName() : "Direction N+1"),
                eval.getInterviewDate() != null ? eval.getInterviewDate() : LocalDate.now(),
                displayScore,
                eval.getFeedback(),
                objectiveItems
        );

        return pdfGeneratorService.generateAnnualEvaluationPdf(pdfData);
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
        List<EvaluationDto.SmartObjectiveDto> objDtos = e.getObjectives().stream()
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

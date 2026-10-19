package com.careertrack.modules.evaluation.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    @Mock
    private EvaluationCampaignRepository campaignRepository;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private ObjectiveRepository objectiveRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EvaluationScoreCalculator scoreCalculator;
    @Mock
    private PdfGeneratorService pdfGeneratorService;

    private EvaluationServiceImpl evaluationService;

    @BeforeEach
    void setUp() {
        evaluationService = new EvaluationServiceImpl(
                campaignRepository,
                evaluationRepository,
                objectiveRepository,
                employeeRepository,
                scoreCalculator,
                pdfGeneratorService
        );
    }

    @Test
    @DisplayName("Lister toutes les campagnes d'évaluation")
    void shouldGetAllCampaigns() {
        EvaluationCampaign campaign = EvaluationCampaign.builder()
                .id(UUID.randomUUID())
                .title("Campagne Annuelle 2026")
                .campaignYear(2026)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 12, 15))
                .status(CampaignStatus.PLANNED)
                .build();

        when(campaignRepository.findAll()).thenReturn(List.of(campaign));

        List<EvaluationCampaignDto> result = evaluationService.getAllCampaigns();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Campagne Annuelle 2026");
    }

    @Test
    @DisplayName("Créer une nouvelle campagne")
    void shouldCreateCampaign() {
        EvaluationCampaignDto dto = EvaluationCampaignDto.builder()
                .title("Campagne 2026")
                .campaignYear(2026)
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 11, 30))
                .status(CampaignStatus.PLANNED)
                .build();

        EvaluationCampaign saved = EvaluationCampaign.builder()
                .id(UUID.randomUUID())
                .title(dto.getTitle())
                .campaignYear(dto.getCampaignYear())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(dto.getStatus())
                .build();

        when(campaignRepository.save(any(EvaluationCampaign.class))).thenReturn(saved);

        EvaluationCampaignDto result = evaluationService.createCampaign(dto);

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getTitle()).isEqualTo("Campagne 2026");
    }

    @Test
    @DisplayName("Clôturer une campagne d'évaluation")
    void shouldCloseCampaign() {
        UUID campaignId = UUID.randomUUID();
        EvaluationCampaign campaign = EvaluationCampaign.builder()
                .id(campaignId)
                .title("Campagne 2026")
                .status(CampaignStatus.IN_PROGRESS)
                .build();

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(EvaluationCampaign.class))).thenAnswer(i -> i.getArgument(0));

        EvaluationCampaignDto closed = evaluationService.closeCampaign(campaignId);

        assertThat(closed.getStatus()).isEqualTo(CampaignStatus.CLOSED);
    }

    @Test
    @DisplayName("Initialiser une fiche d'évaluation avec objectifs SMART")
    void shouldCreateEvaluationWithObjectives() {
        UUID campaignId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        EvaluationCampaign campaign = EvaluationCampaign.builder().id(campaignId).title("Campagne 2026").build();
        Employee manager = Employee.builder().id(managerId).firstName("Cheikh").lastName("Seck").build();
        Employee emp = Employee.builder().id(employeeId).firstName("Amadou").lastName("Diop").manager(manager).build();

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(emp));

        Evaluation savedEval = Evaluation.builder()
                .id(UUID.randomUUID())
                .campaign(campaign)
                .employee(emp)
                .evaluator(manager)
                .status(EvaluationStatus.DRAFT)
                .objectives(new ArrayList<>())
                .build();

        when(evaluationRepository.save(any(Evaluation.class))).thenReturn(savedEval);

        EvaluationCreateDto dto = EvaluationCreateDto.builder()
                .campaignId(campaignId)
                .employeeId(employeeId)
                .objectives(List.of(
                        EvaluationCreateDto.ObjectiveInputDto.builder()
                                .title("Livraison module paie")
                                .weight(BigDecimal.valueOf(40))
                                .category(ObjectiveCategory.OPERATIONAL)
                                .build()
                ))
                .build();

        EvaluationDto result = evaluationService.createEvaluation(dto);

        assertThat(result).isNotNull();
        assertThat(result.getEmployeeFullName()).isEqualTo("Amadou Diop");
        assertThat(result.getEvaluatorFullName()).isEqualTo("Cheikh Seck");
    }

    @Test
    @DisplayName("Soumettre l'auto-évaluation du collaborateur")
    void shouldSubmitSelfEvaluation() {
        UUID evalId = UUID.randomUUID();
        UUID objId = UUID.randomUUID();

        Objective obj = Objective.builder()
                .id(objId)
                .title("Objectif KPI")
                .achievementRate(BigDecimal.ZERO)
                .build();

        Evaluation eval = Evaluation.builder()
                .id(evalId)
                .status(EvaluationStatus.DRAFT)
                .objectives(new ArrayList<>(List.of(obj)))
                .build();

        when(evaluationRepository.findById(evalId)).thenReturn(Optional.of(eval));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArgument(0));

        EvaluationSelfSubmitDto dto = EvaluationSelfSubmitDto.builder()
                .selfScore(new BigDecimal("4.2"))
                .selfFeedback("Bonne progression globale")
                .objectiveRates(List.of(
                        EvaluationSelfSubmitDto.ObjectiveRateDto.builder()
                                .objectiveId(objId)
                                .selfRate(new BigDecimal("90.0"))
                                .build()
                ))
                .build();

        EvaluationDto result = evaluationService.submitSelfEvaluation(evalId, dto, UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(EvaluationStatus.SELF_SUBMITTED);
        assertThat(result.getSelfScore()).isEqualByComparingTo("4.2");
        assertThat(result.getFeedback()).contains("[Auto-évaluation Salarié]");
        assertThat(obj.getAchievementRate()).isEqualByComparingTo("90.0");
    }

    @Test
    @DisplayName("Soumettre l'évaluation managériale avec calcul automatique du score pondéré")
    void shouldSubmitManagerEvaluationAndCalculateScore() {
        UUID evalId = UUID.randomUUID();
        UUID objId = UUID.randomUUID();

        Objective obj = Objective.builder()
                .id(objId)
                .title("Objectif Production")
                .weight(new BigDecimal("100.0"))
                .achievementRate(BigDecimal.ZERO)
                .build();

        Evaluation eval = Evaluation.builder()
                .id(evalId)
                .status(EvaluationStatus.SELF_SUBMITTED)
                .objectives(new ArrayList<>(List.of(obj)))
                .build();

        when(evaluationRepository.findById(evalId)).thenReturn(Optional.of(eval));
        when(scoreCalculator.calculateWeightedScore(any())).thenReturn(new BigDecimal("4.75"));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArgument(0));

        EvaluationManagerSubmitDto dto = EvaluationManagerSubmitDto.builder()
                .managerScore(new BigDecimal("4.5"))
                .managerFeedback("Excellent travail sur l'architecture")
                .interviewDate(LocalDate.of(2026, 11, 20))
                .objectiveRates(List.of(
                        EvaluationManagerSubmitDto.ManagerObjectiveRateDto.builder()
                                .objectiveId(objId)
                                .achievementRate(new BigDecimal("95.0"))
                                .status("ACHIEVED")
                                .build()
                ))
                .build();

        EvaluationDto result = evaluationService.submitManagerEvaluation(evalId, dto, UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(EvaluationStatus.MANAGER_EVALUATED);
        assertThat(result.getManagerScore()).isEqualByComparingTo("4.5");
        assertThat(result.getFinalScore()).isEqualByComparingTo("4.75");
        assertThat(result.getFeedback()).contains("[Évaluation Manager N+1]");
        verify(scoreCalculator, times(1)).calculateWeightedScore(eval.getObjectives());
    }

    @Test
    @DisplayName("Valider et clôturer définitivement une évaluation")
    void shouldCompleteEvaluation() {
        UUID evalId = UUID.randomUUID();
        Evaluation eval = Evaluation.builder()
                .id(evalId)
                .status(EvaluationStatus.MANAGER_EVALUATED)
                .finalScore(new BigDecimal("4.5"))
                .objectives(new ArrayList<>())
                .build();

        when(evaluationRepository.findById(evalId)).thenReturn(Optional.of(eval));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArgument(0));

        EvaluationCompleteDto dto = EvaluationCompleteDto.builder()
                .finalFeedback("Entretien validé conjointement")
                .build();

        EvaluationDto result = evaluationService.completeEvaluation(evalId, dto);

        assertThat(result.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
        assertThat(result.getSummaryPdfUrl()).isEqualTo("/api/v1/evaluations/" + evalId + "/pdf");
        assertThat(result.getFeedback()).contains("[Validation Finale]");
    }

    @Test
    @DisplayName("Générer le PDF de bilan d'entretien annuel signable")
    void shouldGenerateEvaluationPdf() {
        UUID evalId = UUID.randomUUID();
        Employee emp = Employee.builder()
                .id(UUID.randomUUID())
                .firstName("Moussa")
                .lastName("Ndiaye")
                .employeeNumber("MAT-101")
                .build();

        Evaluation eval = Evaluation.builder()
                .id(evalId)
                .employee(emp)
                .finalScore(new BigDecimal("4.6"))
                .feedback("Compte-rendu")
                .objectives(new ArrayList<>())
                .build();

        byte[] expectedPdf = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF

        when(evaluationRepository.findById(evalId)).thenReturn(Optional.of(eval));
        when(pdfGeneratorService.generateAnnualEvaluationPdf(any(AnnualEvaluationPdfData.class))).thenReturn(expectedPdf);

        byte[] result = evaluationService.generateEvaluationPdf(evalId);

        assertThat(result).isEqualTo(expectedPdf);
        ArgumentCaptor<AnnualEvaluationPdfData> captor = ArgumentCaptor.forClass(AnnualEvaluationPdfData.class);
        verify(pdfGeneratorService).generateAnnualEvaluationPdf(captor.capture());
        assertThat(captor.getValue().employeeName()).isEqualTo("Moussa Ndiaye");
        assertThat(captor.getValue().employeeNumber()).isEqualTo("MAT-101");
    }
}

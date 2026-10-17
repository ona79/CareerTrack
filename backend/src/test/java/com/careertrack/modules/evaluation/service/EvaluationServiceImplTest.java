package com.careertrack.modules.evaluation.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.evaluation.dto.EvaluationCampaignDto;
import com.careertrack.modules.evaluation.dto.EvaluationDto;
import com.careertrack.modules.evaluation.entity.*;
import com.careertrack.modules.evaluation.repository.EvaluationCampaignRepository;
import com.careertrack.modules.evaluation.repository.EvaluationRepository;
import com.careertrack.modules.evaluation.repository.ObjectiveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    private EvaluationServiceImpl evaluationService;

    @BeforeEach
    void setUp() {
        evaluationService = new EvaluationServiceImpl(campaignRepository, evaluationRepository, objectiveRepository);
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
    @DisplayName("Obtenir les évaluations par campagne avec objectifs SMART")
    void shouldGetEvaluationsByCampaign() {
        UUID campaignId = UUID.randomUUID();
        UUID evalId = UUID.randomUUID();

        EvaluationCampaign campaign = EvaluationCampaign.builder().id(campaignId).title("Campagne 2026").build();
        Employee emp = Employee.builder().id(UUID.randomUUID()).firstName("Aminata").lastName("Diallo").build();

        Evaluation eval = Evaluation.builder()
                .id(evalId)
                .campaign(campaign)
                .employee(emp)
                .status(EvaluationStatus.DRAFT)
                .finalScore(new BigDecimal("4.5"))
                .build();

        Objective obj = Objective.builder()
                .id(UUID.randomUUID())
                .evaluation(eval)
                .title("Objectif Chiffre d'Affaires")
                .category(ObjectiveCategory.OPERATIONAL)
                .weight(new BigDecimal("30.0"))
                .status("IN_PROGRESS")
                .build();

        when(evaluationRepository.findByCampaignId(campaignId)).thenReturn(List.of(eval));
        when(objectiveRepository.findByEvaluationId(evalId)).thenReturn(List.of(obj));

        List<EvaluationDto> result = evaluationService.getEvaluationsByCampaign(campaignId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmployeeFullName()).isEqualTo("Aminata Diallo");
        assertThat(result.get(0).getObjectives()).hasSize(1);
    }

    @Test
    @DisplayName("Obtenir une évaluation par ID ou lancer une exception")
    void shouldGetEvaluationById() {
        UUID evalId = UUID.randomUUID();
        Evaluation eval = Evaluation.builder()
                .id(evalId)
                .status(EvaluationStatus.COMPLETED)
                .finalScore(new BigDecimal("4.8"))
                .build();

        when(evaluationRepository.findById(evalId)).thenReturn(Optional.of(eval));
        when(objectiveRepository.findByEvaluationId(evalId)).thenReturn(Collections.emptyList());

        EvaluationDto result = evaluationService.getEvaluationById(evalId);

        assertThat(result.getId()).isEqualTo(evalId);
        assertThat(result.getFinalScore()).isEqualByComparingTo("4.8");

        when(evaluationRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> evaluationService.getEvaluationById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

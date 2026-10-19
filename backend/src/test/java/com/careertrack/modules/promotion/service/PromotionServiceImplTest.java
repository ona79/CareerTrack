package com.careertrack.modules.promotion.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.Position;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.employee.repository.PositionRepository;
import com.careertrack.modules.promotion.dto.PromotionProposalDto;
import com.careertrack.modules.promotion.entity.PromotionProposal;
import com.careertrack.modules.promotion.entity.PromotionStatus;
import com.careertrack.modules.promotion.repository.PromotionProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    @Mock
    private PromotionProposalRepository proposalRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PositionRepository positionRepository;
    @Mock
    private com.careertrack.modules.employee.repository.ContractRepository contractRepository;
    @Mock
    private com.careertrack.modules.employee.repository.SalaryHistoryRepository salaryHistoryRepository;
    @Mock
    private com.careertrack.modules.employee.repository.PositionHistoryRepository positionHistoryRepository;
    @Mock
    private com.careertrack.common.security.EmployeeSecurityService employeeSecurity;

    private PromotionServiceImpl promotionService;

    @BeforeEach
    void setUp() {
        promotionService = new PromotionServiceImpl(
                proposalRepository,
                employeeRepository,
                positionRepository,
                contractRepository,
                salaryHistoryRepository,
                positionHistoryRepository,
                employeeSecurity
        );
    }

    @Test
    @DisplayName("Lister toutes les propositions de promotion")
    void shouldGetAllProposals() {
        Employee emp = Employee.builder().id(UUID.randomUUID()).firstName("Cheikh").lastName("Fall").build();
        Position pos = Position.builder().id(UUID.randomUUID()).title("Lead Développeur").build();

        PromotionProposal p = PromotionProposal.builder()
                .id(UUID.randomUUID())
                .employee(emp)
                .targetPosition(pos)
                .currentSalary(new BigDecimal("800000"))
                .proposedSalary(new BigDecimal("1000000"))
                .status(PromotionStatus.SUBMITTED)
                .build();

        when(proposalRepository.findAll()).thenReturn(List.of(p));

        List<PromotionProposalDto> result = promotionService.getAllProposals();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProposedPositionTitle()).isEqualTo("Lead Développeur");
        assertThat(result.get(0).getPercentageIncrease()).isNotNull();
    }

    @Test
    @DisplayName("Créer une proposition de promotion avec calcul du pourcentage")
    void shouldCreateProposal() {
        UUID empId = UUID.randomUUID();
        UUID posId = UUID.randomUUID();

        Employee emp = Employee.builder().id(empId).firstName("Fatou").lastName("Sow").build();
        Position pos = Position.builder().id(posId).title("Manager").build();

        PromotionProposalDto dto = PromotionProposalDto.builder()
                .employeeId(empId)
                .proposedPositionId(posId)
                .currentSalary(new BigDecimal("500000"))
                .proposedSalary(new BigDecimal("600000"))
                .justification("Excellente performance annuelle")
                .build();

        PromotionProposal saved = PromotionProposal.builder()
                .id(UUID.randomUUID())
                .employee(emp)
                .targetPosition(pos)
                .currentSalary(dto.getCurrentSalary())
                .proposedSalary(dto.getProposedSalary())
                .status(PromotionStatus.SUBMITTED)
                .build();

        when(employeeRepository.findById(empId)).thenReturn(Optional.of(emp));
        when(positionRepository.findById(posId)).thenReturn(Optional.of(pos));
        when(proposalRepository.save(any(PromotionProposal.class))).thenReturn(saved);

        PromotionProposalDto result = promotionService.createProposal(dto);

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getProposedPositionTitle()).isEqualTo("Manager");
    }

    @Test
    @DisplayName("Valider ou rejeter une proposition de promotion")
    void shouldUpdateProposalStatus() {
        UUID proposalId = UUID.randomUUID();
        Employee emp = Employee.builder().id(UUID.randomUUID()).firstName("Aliou").lastName("Ba").build();
        Position pos = Position.builder().id(UUID.randomUUID()).title("Senior").build();

        PromotionProposal proposal = PromotionProposal.builder()
                .id(proposalId)
                .employee(emp)
                .targetPosition(pos)
                .currentSalary(new BigDecimal("700000"))
                .proposedSalary(new BigDecimal("850000"))
                .status(PromotionStatus.SUBMITTED)
                .build();

        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any(PromotionProposal.class))).thenAnswer(inv -> inv.getArgument(0));

        PromotionProposalDto result = promotionService.updateStatus(proposalId, PromotionStatus.APPROVED, "Approuvé");

        assertThat(result.getStatus()).isEqualTo(PromotionStatus.APPROVED);
        assertThat(proposal.getStatus()).isEqualTo(PromotionStatus.APPROVED);
    }
}

package com.careertrack.modules.promotion.service;

import com.careertrack.common.security.EmployeeSecurityService;
import com.careertrack.modules.auth.repository.UserRepository;
import com.careertrack.modules.employee.entity.Contract;
import com.careertrack.modules.employee.entity.Department;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.Position;
import com.careertrack.modules.employee.repository.*;
import com.careertrack.modules.promotion.dto.PromotionProposalDto;
import com.careertrack.modules.promotion.dto.SalarySimulationRequestDto;
import com.careertrack.modules.promotion.dto.SalarySimulationResultDto;
import com.careertrack.modules.promotion.entity.PromotionProposal;
import com.careertrack.modules.promotion.entity.PromotionStatus;
import com.careertrack.modules.promotion.entity.SalarySimulation;
import com.careertrack.modules.promotion.repository.PromotionProposalRepository;
import com.careertrack.modules.promotion.repository.SalarySimulationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    @Mock
    private PromotionProposalRepository proposalRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PositionRepository positionRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private SalaryHistoryRepository salaryHistoryRepository;
    @Mock
    private PositionHistoryRepository positionHistoryRepository;
    @Mock
    private EmployeeSecurityService employeeSecurity;
    @Mock
    private SalarySimulationRepository salarySimulationRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private UserRepository userRepository;

    private SalarySimulationCalculator simulationCalculator = new SalarySimulationCalculator();

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
                employeeSecurity,
                salarySimulationRepository,
                simulationCalculator,
                departmentRepository,
                userRepository
        );
    }

    @Test
    @DisplayName("Lister toutes les propositions de promotion avec visibilité salaire pour DRH")
    void shouldGetAllProposalsWithSalaryWhenDrh() {
        when(employeeSecurity.hasRole("DRH")).thenReturn(true);

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
        assertThat(result.get(0).getCurrentSalary()).isEqualTo(new BigDecimal("800000"));
        assertThat(result.get(0).getProposedSalary()).isEqualTo(new BigDecimal("1000000"));
        assertThat(result.get(0).getPercentageIncrease()).isNotNull();
    }

    @Test
    @DisplayName("Masquer les salaires si l'utilisateur n'est pas DRH ou DIRECTION")
    void shouldMaskSalariesWhenNotDrhOrDirection() {
        when(employeeSecurity.hasRole("DRH")).thenReturn(false);
        when(employeeSecurity.hasRole("DIRECTION")).thenReturn(false);

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
        assertThat(result.get(0).getCurrentSalary()).isNull();
        assertThat(result.get(0).getProposedSalary()).isNull();
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

    @Test
    @DisplayName("Simuler l'impact sur la masse salariale en FCFA")
    void shouldSimulateSalaryImpactInFcfa() {
        Department itDept = Department.builder().id(UUID.randomUUID()).name("Informatique").build();
        Employee emp1 = Employee.builder().id(UUID.randomUUID()).department(itDept).build();
        Employee emp2 = Employee.builder().id(UUID.randomUUID()).department(itDept).build();

        Contract c1 = Contract.builder().id(UUID.randomUUID()).employee(emp1).grossSalary(new BigDecimal("1000000")).active(true).build();
        Contract c2 = Contract.builder().id(UUID.randomUUID()).employee(emp2).grossSalary(new BigDecimal("2000000")).active(true).build();

        when(departmentRepository.findById(itDept.getId())).thenReturn(Optional.of(itDept));
        when(contractRepository.findByActiveTrue()).thenReturn(List.of(c1, c2));
        when(salarySimulationRepository.save(any(SalarySimulation.class))).thenAnswer(inv -> {
            SalarySimulation s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SalarySimulationRequestDto request = SalarySimulationRequestDto.builder()
                .title("Revalorisation IT 2026")
                .departmentId(itDept.getId())
                .averageIncreasePercentage(new BigDecimal("10.0"))
                .build();

        SalarySimulationResultDto result = promotionService.simulateSalaryImpact(request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getDepartmentName()).isEqualTo("Informatique");
        // Masse actuelle: 1 000 000 + 2 000 000 = 3 000 000 FCFA
        assertThat(result.getCurrentMonthlyPayroll()).isEqualByComparingTo("3000000.00");
        // Augmentation 10% = 300 000 FCFA
        assertThat(result.getMonthlyDifference()).isEqualByComparingTo("300000.00");
        // Masse projetée = 3 300 000 FCFA
        assertThat(result.getProjectedMonthlyPayroll()).isEqualByComparingTo("3300000.00");
        // Impact annuel = 300 000 * 12 = 3 600 000 FCFA
        assertThat(result.getProjectedAnnualPayrollImpact()).isEqualByComparingTo("3600000.00");
        assertThat(result.getAffectedEmployeesCount()).isEqualTo(2);
    }
}

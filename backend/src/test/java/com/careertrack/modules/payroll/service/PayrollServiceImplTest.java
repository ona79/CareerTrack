package com.careertrack.modules.payroll.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.entity.Contract;
import com.careertrack.modules.employee.entity.Department;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.EmployeeStatus;
import com.careertrack.modules.employee.repository.ContractRepository;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.payroll.dto.PayrollGenerationResultDto;
import com.careertrack.modules.payroll.dto.PayrollParameterResponseDto;
import com.careertrack.modules.payroll.dto.PayrollStatsResponseDto;
import com.careertrack.modules.payroll.dto.PayslipResponseDto;
import com.careertrack.modules.payroll.entity.*;
import com.careertrack.modules.payroll.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceImplTest {

    @Mock
    private PayslipRepository payslipRepository;

    @Mock
    private PayslipLineRepository payslipLineRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PayrollParameterRepository payrollParameterRepository;

    @Mock
    private PayrollTaxBracketRepository payrollTaxBracketRepository;

    @Mock
    private PayrollTrimfBracketRepository payrollTrimfBracketRepository;

    private PayrollCalculator payrollCalculator;
    private PayrollServiceImpl payrollService;

    private Employee employee1;
    private Employee employee2;
    private Department techDept;
    private Department hrDept;

    @BeforeEach
    void setUp() {
        payrollCalculator = new PayrollCalculator();
        payrollService = new PayrollServiceImpl(
                payslipRepository,
                payslipLineRepository,
                employeeRepository,
                contractRepository,
                payrollParameterRepository,
                payrollTaxBracketRepository,
                payrollTrimfBracketRepository,
                payrollCalculator
        );

        techDept = Department.builder().id(UUID.randomUUID()).name("Ingénierie").build();
        hrDept = Department.builder().id(UUID.randomUUID()).name("Ressources Humaines").build();

        employee1 = Employee.builder()
                .id(UUID.randomUUID())
                .employeeNumber("EMP-001")
                .firstName("Mamadou")
                .lastName("Diallo")
                .department(techDept)
                .status(EmployeeStatus.ACTIVE)
                .isCadre(true)
                .maritalStatus("CELIBATAIRE")
                .dependentsCount(0)
                .build();

        employee2 = Employee.builder()
                .id(UUID.randomUUID())
                .employeeNumber("EMP-002")
                .firstName("Awa")
                .lastName("Ndiaye")
                .department(hrDept)
                .status(EmployeeStatus.ACTIVE)
                .isCadre(false)
                .maritalStatus("MARIE")
                .dependentsCount(2)
                .build();

        // Default mock parameters with lenient strictness
        PayrollParameter p1 = PayrollParameter.builder().key("IPRES_RG_EMPLOYEE_RATE").value("0.056").validFrom(LocalDate.of(2020, 1, 1)).build();
        PayrollParameter p2 = PayrollParameter.builder().key("IPRES_RG_EMPLOYER_RATE").value("0.084").validFrom(LocalDate.of(2020, 1, 1)).build();
        PayrollParameter p3 = PayrollParameter.builder().key("IPRES_RG_CEILING").value("432000").validFrom(LocalDate.of(2020, 1, 1)).build();
        lenient().when(payrollParameterRepository.findAllValidAt(any(LocalDate.class))).thenReturn(List.of(p1, p2, p3));
        lenient().when(payrollTaxBracketRepository.findActiveBracketsAt(any(LocalDate.class))).thenReturn(Collections.emptyList());
        lenient().when(payrollTrimfBracketRepository.findActiveBracketsAt(any(LocalDate.class))).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("Génération de la paie mensuelle : traite les actifs avec contrat, calcule et persiste les bulletins")
    void shouldGeneratePayrollForPeriod() {
        Contract c1 = Contract.builder().id(UUID.randomUUID()).employee(employee1).grossSalary(new BigDecimal("950000")).active(true).build();
        Contract c2 = Contract.builder().id(UUID.randomUUID()).employee(employee2).grossSalary(new BigDecimal("300000")).active(true).build();

        when(employeeRepository.findAll()).thenReturn(List.of(employee1, employee2));
        when(contractRepository.findByEmployeeIdAndActiveTrue(employee1.getId())).thenReturn(Optional.of(c1));
        when(contractRepository.findByEmployeeIdAndActiveTrue(employee2.getId())).thenReturn(Optional.of(c2));

        when(payslipRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(any(UUID.class), eq(2026), eq(9)))
                .thenReturn(Optional.empty());
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> {
            Payslip p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });

        PayrollGenerationResultDto result = payrollService.generatePayrollForPeriod("2026-09");

        assertThat(result.getTotalProcessed()).isEqualTo(2);
        assertThat(result.getTotalGenerated()).isEqualTo(2);
        assertThat(result.getTotalSkipped()).isEqualTo(0);
        assertThat(result.getPayslips()).hasSize(2);

        verify(payslipRepository, times(2)).save(any(Payslip.class));
        verify(payslipLineRepository, times(2)).saveAll(any());
    }

    @Test
    @DisplayName("Idempotence : préserve les bulletins déjà VALIDATED ou PAID sans les écraser")
    void shouldPreserveValidatedAndPaidPayslipsDuringGeneration() {
        Contract c1 = Contract.builder().id(UUID.randomUUID()).employee(employee1).grossSalary(new BigDecimal("950000")).active(true).build();

        Payslip validatedPayslip = Payslip.builder()
                .id(UUID.randomUUID())
                .employee(employee1)
                .periodYear(2026)
                .periodMonth(9)
                .grossSalary(new BigDecimal("950000"))
                .netSalary(new BigDecimal("651452"))
                .totalDeductions(new BigDecimal("298548"))
                .status(PayslipStatus.VALIDATED)
                .build();

        when(employeeRepository.findAll()).thenReturn(List.of(employee1));
        when(contractRepository.findByEmployeeIdAndActiveTrue(employee1.getId())).thenReturn(Optional.of(c1));
        when(payslipRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(employee1.getId(), 2026, 9))
                .thenReturn(Optional.of(validatedPayslip));

        PayrollGenerationResultDto result = payrollService.generatePayrollForPeriod("2026-09");

        assertThat(result.getTotalProcessed()).isEqualTo(1);
        assertThat(result.getTotalGenerated()).isEqualTo(0);
        assertThat(result.getTotalSkipped()).isEqualTo(1);
        // Aucune sauvegarde pour le bulletin validé
        verify(payslipRepository, never()).save(any(Payslip.class));
    }

    @Test
    @DisplayName("Régénération : un bulletin CANCELLED ou GENERATED est recalculé et mis à jour")
    void shouldRegenerateCancelledPayslip() {
        Contract c1 = Contract.builder().id(UUID.randomUUID()).employee(employee1).grossSalary(new BigDecimal("950000")).active(true).build();

        Payslip cancelledPayslip = Payslip.builder()
                .id(UUID.randomUUID())
                .employee(employee1)
                .periodYear(2026)
                .periodMonth(9)
                .grossSalary(new BigDecimal("950000"))
                .netSalary(new BigDecimal("651452"))
                .totalDeductions(new BigDecimal("298548"))
                .status(PayslipStatus.CANCELLED)
                .cancelledBy("drh.user")
                .cancelledAt(java.time.Instant.now())
                .build();

        when(employeeRepository.findAll()).thenReturn(List.of(employee1));
        when(contractRepository.findByEmployeeIdAndActiveTrue(employee1.getId())).thenReturn(Optional.of(c1));
        when(payslipRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(employee1.getId(), 2026, 9))
                .thenReturn(Optional.of(cancelledPayslip));
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));

        PayrollGenerationResultDto result = payrollService.generatePayrollForPeriod("2026-09");

        assertThat(result.getTotalGenerated()).isEqualTo(1);
        assertThat(cancelledPayslip.getStatus()).isEqualTo(PayslipStatus.GENERATED);
        assertThat(cancelledPayslip.getCancelledBy()).isNull();
        assertThat(cancelledPayslip.getCancelledAt()).isNull();

        verify(payslipRepository, times(1)).save(cancelledPayslip);
    }

    @Test
    @DisplayName("Cycle de vie et transition de statut : GENERATED -> VALIDATED -> CANCELLED / PAID")
    void shouldHandleStatusTransitionsAndAuditCancellation() {
        UUID payslipId = UUID.randomUUID();
        Payslip payslip = Payslip.builder()
                .id(payslipId)
                .employee(employee1)
                .status(PayslipStatus.GENERATED)
                .periodYear(2026)
                .periodMonth(9)
                .build();

        when(payslipRepository.findById(payslipId)).thenReturn(Optional.of(payslip));
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. GENERATED -> VALIDATED
        PayslipResponseDto validatedDto = payrollService.updateStatus(payslipId, PayslipStatus.VALIDATED);
        assertThat(validatedDto.getStatus()).isEqualTo(PayslipStatus.VALIDATED);

        // 2. VALIDATED -> CANCELLED avec audit
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("drh.ndiaye", null));

        PayslipResponseDto cancelledDto = payrollService.updateStatus(payslipId, PayslipStatus.CANCELLED);
        assertThat(cancelledDto.getStatus()).isEqualTo(PayslipStatus.CANCELLED);
        assertThat(payslip.getCancelledBy()).isEqualTo("drh.ndiaye");
        assertThat(payslip.getCancelledAt()).isNotNull();

        // 3. Un bulletin PAID ne peut pas être modifié
        payslip.setStatus(PayslipStatus.PAID);
        assertThatThrownBy(() -> payrollService.updateStatus(payslipId, PayslipStatus.CANCELLED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PAYÉ est immuable");
    }

    @Test
    @DisplayName("Masse salariale : effectif total < 3 ne renvoie que les totaux globaux sans ventilation par département")
    void shouldReturnOnlyTotalsWhenTotalEmployeesUnderThree() {
        Payslip p1 = Payslip.builder().id(UUID.randomUUID()).employee(employee1).grossSalary(new BigDecimal("500000")).netSalary(new BigDecimal("400000")).totalDeductions(new BigDecimal("100000")).employerContributions(new BigDecimal("50000")).status(PayslipStatus.PAID).build();
        Payslip p2 = Payslip.builder().id(UUID.randomUUID()).employee(employee2).grossSalary(new BigDecimal("400000")).netSalary(new BigDecimal("320000")).totalDeductions(new BigDecimal("80000")).employerContributions(new BigDecimal("40000")).status(PayslipStatus.PAID).build();

        when(payslipRepository.findByPeriodYearAndPeriodMonth(2026, 9)).thenReturn(List.of(p1, p2));

        PayrollStatsResponseDto stats = payrollService.getPayrollStats("2026-09");

        assertThat(stats.getTotalEmployeesPaid()).isEqualTo(2);
        assertThat(stats.getTotalGrossSalary()).isEqualByComparingTo("900000");
        assertThat(stats.getTotalNetSalary()).isEqualByComparingTo("720000");
        assertThat(stats.getTotalEmployerCost()).isEqualByComparingTo("990000");
        assertThat(stats.getDepartmentBreakdown()).isEmpty(); // Aucun détail par département
    }

    @Test
    @DisplayName("Masse salariale : départements < 3 groupés dans 'Autres' ; si 'Autres' < 3 masqué, si >= 3 affiché")
    void shouldGroupSmallDepartmentsIntoOthersAccordingToConfidentiality() {
        // Département A : 3 employés
        Department deptA = Department.builder().id(UUID.randomUUID()).name("Dept A").build();
        // Département B : 1 employé
        Department deptB = Department.builder().id(UUID.randomUUID()).name("Dept B").build();
        // Département C : 1 employé
        Department deptC = Department.builder().id(UUID.randomUUID()).name("Dept C").build();

        Employee eA1 = Employee.builder().id(UUID.randomUUID()).department(deptA).build();
        Employee eA2 = Employee.builder().id(UUID.randomUUID()).department(deptA).build();
        Employee eA3 = Employee.builder().id(UUID.randomUUID()).department(deptA).build();
        Employee eB1 = Employee.builder().id(UUID.randomUUID()).department(deptB).build();
        Employee eC1 = Employee.builder().id(UUID.randomUUID()).department(deptC).build();

        Payslip pA1 = Payslip.builder().employee(eA1).grossSalary(new BigDecimal("100000")).netSalary(new BigDecimal("80000")).totalDeductions(new BigDecimal("20000")).employerContributions(new BigDecimal("10000")).status(PayslipStatus.PAID).build();
        Payslip pA2 = Payslip.builder().employee(eA2).grossSalary(new BigDecimal("100000")).netSalary(new BigDecimal("80000")).totalDeductions(new BigDecimal("20000")).employerContributions(new BigDecimal("10000")).status(PayslipStatus.PAID).build();
        Payslip pA3 = Payslip.builder().employee(eA3).grossSalary(new BigDecimal("100000")).netSalary(new BigDecimal("80000")).totalDeductions(new BigDecimal("20000")).employerContributions(new BigDecimal("10000")).status(PayslipStatus.PAID).build();
        Payslip pB1 = Payslip.builder().employee(eB1).grossSalary(new BigDecimal("150000")).netSalary(new BigDecimal("120000")).totalDeductions(new BigDecimal("30000")).employerContributions(new BigDecimal("15000")).status(PayslipStatus.PAID).build();
        Payslip pC1 = Payslip.builder().employee(eC1).grossSalary(new BigDecimal("150000")).netSalary(new BigDecimal("120000")).totalDeductions(new BigDecimal("30000")).employerContributions(new BigDecimal("15000")).status(PayslipStatus.PAID).build();

        // Total = 5 employés. Dept A a 3. Dept B (1) et Dept C (1) = 2 au total pour "Autres".
        // Comme "Autres" a 2 employés (< 3), "Autres" est masqué.
        when(payslipRepository.findByPeriodYearAndPeriodMonth(2026, 9))
                .thenReturn(List.of(pA1, pA2, pA3, pB1, pC1));

        PayrollStatsResponseDto stats1 = payrollService.getPayrollStats("2026-09");
        assertThat(stats1.getTotalEmployeesPaid()).isEqualTo(5);
        assertThat(stats1.getDepartmentBreakdown()).hasSize(1);
        assertThat(stats1.getDepartmentBreakdown().get(0).getDepartmentName()).isEqualTo("Dept A");

        // Ajoutons un 3ème employé dans une petite équipe (Dept D) -> "Autres" atteint 3 employés
        Department deptD = Department.builder().id(UUID.randomUUID()).name("Dept D").build();
        Employee eD1 = Employee.builder().id(UUID.randomUUID()).department(deptD).build();
        Payslip pD1 = Payslip.builder().employee(eD1).grossSalary(new BigDecimal("120000")).netSalary(new BigDecimal("90000")).totalDeductions(new BigDecimal("30000")).employerContributions(new BigDecimal("12000")).status(PayslipStatus.PAID).build();

        when(payslipRepository.findByPeriodYearAndPeriodMonth(2026, 9))
                .thenReturn(List.of(pA1, pA2, pA3, pB1, pC1, pD1));

        PayrollStatsResponseDto stats2 = payrollService.getPayrollStats("2026-09");
        assertThat(stats2.getTotalEmployeesPaid()).isEqualTo(6);
        assertThat(stats2.getDepartmentBreakdown()).hasSize(2);
        assertThat(stats2.getDepartmentBreakdown().get(0).getDepartmentName()).isEqualTo("Autres");
        assertThat(stats2.getDepartmentBreakdown().get(0).getEmployeeCount()).isEqualTo(3);
        assertThat(stats2.getDepartmentBreakdown().get(1).getDepartmentName()).isEqualTo("Dept A");
    }

    @Test
    @DisplayName("Date d'effet : applique les paramètres en vigueur au dernier jour du mois")
    void shouldApplyParametersEffectiveAtEndOfMonth() {
        // En mai 2026, taux IPRES = 5.6%
        // En juin 2026, nouveau taux IPRES = 6.0% (validFrom = 2026-06-01)
        PayrollParameter oldParam = PayrollParameter.builder().key("IPRES_RG_EMPLOYEE_RATE").value("0.056").validFrom(LocalDate.of(2020, 1, 1)).build();
        PayrollParameter newParam = PayrollParameter.builder().key("IPRES_RG_EMPLOYEE_RATE").value("0.060").validFrom(LocalDate.of(2026, 6, 1)).build();
        PayrollParameter ipmZero = PayrollParameter.builder().key("IPM_EMPLOYEE_RATE").value("0.0").validFrom(LocalDate.of(2020, 1, 1)).build();

        // Pour mai 2026 (dernier jour 2026-05-31), la requête renvoie oldParam
        when(payrollParameterRepository.findAllValidAt(LocalDate.of(2026, 5, 31))).thenReturn(List.of(oldParam, ipmZero));
        // Pour juin 2026 (dernier jour 2026-06-30), la requête renvoie newParam puis oldParam
        when(payrollParameterRepository.findAllValidAt(LocalDate.of(2026, 6, 30))).thenReturn(List.of(newParam, oldParam, ipmZero));

        Contract c1 = Contract.builder().id(UUID.randomUUID()).employee(employee1).grossSalary(new BigDecimal("100000")).active(true).build();
        when(employeeRepository.findAll()).thenReturn(List.of(employee1));
        when(contractRepository.findByEmployeeIdAndActiveTrue(employee1.getId())).thenReturn(Optional.of(c1));
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));

        // Mai 2026
        when(payslipRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(employee1.getId(), 2026, 5)).thenReturn(Optional.empty());
        PayrollGenerationResultDto mayResult = payrollService.generatePayrollForPeriod("2026-05");
        // IPRES à 5.6% sur 100 000 = 5 600
        BigDecimal mayDeductions = mayResult.getPayslips().get(0).getTotalDeductions();
        assertThat(mayDeductions).isEqualByComparingTo("5600");

        // Juin 2026
        when(payslipRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(employee1.getId(), 2026, 6)).thenReturn(Optional.empty());
        PayrollGenerationResultDto juneResult = payrollService.generatePayrollForPeriod("2026-06");
        // IPRES à 6.0% sur 100 000 = 6 000
        BigDecimal juneDeductions = juneResult.getPayslips().get(0).getTotalDeductions();
        assertThat(juneDeductions).isEqualByComparingTo("6000");
    }

    @Test
    @DisplayName("Liste des paramètres de paie en lecture seule")
    void shouldGetPayrollParameters() {
        PayrollParameter p = PayrollParameter.builder()
                .id(UUID.randomUUID())
                .key("IPRES_RG_EMPLOYEE_RATE")
                .value("0.056")
                .category("COTISATION_SOCIALE")
                .status("VALEUR_A_CONFIRMER")
                .source("source à vérifier")
                .validFrom(LocalDate.of(2020, 1, 1))
                .build();

        when(payrollParameterRepository.findAllByOrderByKeyAsc()).thenReturn(List.of(p));

        List<PayrollParameterResponseDto> params = payrollService.getPayrollParameters();

        assertThat(params).hasSize(1);
        assertThat(params.get(0).getKey()).isEqualTo("IPRES_RG_EMPLOYEE_RATE");
        assertThat(params.get(0).getStatus()).isEqualTo("VALEUR_A_CONFIRMER");
    }
}

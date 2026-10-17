package com.careertrack.modules.payroll.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.entity.Contract;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.repository.ContractRepository;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.payroll.dto.PayslipResponseDto;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.entity.PayslipStatus;
import com.careertrack.modules.payroll.repository.PayslipLineRepository;
import com.careertrack.modules.payroll.repository.PayslipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private PayrollServiceImpl payrollService;

    private UUID employeeId;
    private Employee employee;
    private Contract contract;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        employee = Employee.builder()
                .id(employeeId)
                .firstName("Saliou")
                .lastName("Diallo")
                .employeeNumber("EMP-001")
                .build();

        contract = Contract.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .grossSalary(new BigDecimal("1000000"))
                .active(true)
                .build();

        ReflectionTestUtils.setField(payrollService, "ipresEmployeeRate", new BigDecimal("0.056"));
        ReflectionTestUtils.setField(payrollService, "ipresEmployerRate", new BigDecimal("0.084"));
        ReflectionTestUtils.setField(payrollService, "cssEmployerRate", new BigDecimal("0.070"));
        ReflectionTestUtils.setField(payrollService, "irEstimateRate", new BigDecimal("0.12"));
        ReflectionTestUtils.setField(payrollService, "trimfMonthlyAmount", new BigDecimal("900"));
    }

    @Test
    @DisplayName("Génération mensuelle de bulletin avec calculs de cotisations sénégalaises")
    void shouldGenerateMonthlyPayslip() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(contractRepository.findByEmployeeIdAndActiveTrue(employeeId)).thenReturn(Optional.of(contract));
        when(payslipRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(employeeId, 2026, 4))
                .thenReturn(Optional.empty());
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payslip result = payrollService.generateMonthlyPayslip(employeeId, "2026-04");

        assertThat(result).isNotNull();
        assertThat(result.getGrossSalary()).isEqualByComparingTo("1000000");
        // Déductions : IPRES 56 000 + IR 120 000 + TRIMF 900 = 176 900
        assertThat(result.getTotalDeductions()).isEqualByComparingTo("176900");
        // Net : 1 000 000 - 176 900 = 823 100
        assertThat(result.getNetSalary()).isEqualByComparingTo("823100");
        assertThat(result.getStatus()).isEqualTo(PayslipStatus.GENERATED);

        verify(payslipLineRepository, atLeastOnce()).saveAll(any());
        verify(payslipRepository, times(1)).save(any(Payslip.class));
    }

    @Test
    @DisplayName("Génération échoue si l'employé n'existe pas")
    void shouldThrowExceptionWhenEmployeeNotFound() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payrollService.generateMonthlyPayslip(employeeId, "2026-04"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employé introuvable");
    }

    @Test
    @DisplayName("Récupération des bulletins par période")
    void shouldGetPayslipsByPeriod() {
        Payslip p = Payslip.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .periodYear(2026)
                .periodMonth(4)
                .grossSalary(new BigDecimal("500000"))
                .netSalary(new BigDecimal("415000"))
                .totalDeductions(new BigDecimal("85000"))
                .status(PayslipStatus.PAID)
                .paymentDate(LocalDate.of(2026, 4, 30))
                .build();

        when(payslipRepository.findByPeriodYearAndPeriodMonth(2026, 4)).thenReturn(List.of(p));

        List<PayslipResponseDto> dtos = payrollService.getPayslipsByPeriod("2026-04");

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getGrossSalary()).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("Récupération d'un bulletin par ID")
    void shouldGetPayslipById() {
        UUID payslipId = UUID.randomUUID();
        Payslip p = Payslip.builder()
                .id(payslipId)
                .employee(employee)
                .periodYear(2026)
                .periodMonth(3)
                .grossSalary(new BigDecimal("600000"))
                .netSalary(new BigDecimal("500000"))
                .totalDeductions(new BigDecimal("100000"))
                .status(PayslipStatus.GENERATED)
                .build();

        when(payslipRepository.findById(payslipId)).thenReturn(Optional.of(p));

        PayslipResponseDto dto = payrollService.getPayslipById(payslipId);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(payslipId);
    }
}

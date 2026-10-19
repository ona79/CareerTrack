package com.careertrack.modules.dashboard.service;

import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.dashboard.dto.DashboardStatsDto;
import com.careertrack.modules.employee.entity.*;
import com.careertrack.modules.employee.repository.*;
import com.careertrack.modules.leave.entity.LeaveRequest;
import com.careertrack.modules.leave.entity.LeaveStatus;
import com.careertrack.modules.leave.repository.LeaveRequestRepository;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.repository.PayslipRepository;
import com.careertrack.modules.training.entity.TrainingSession;
import com.careertrack.modules.training.entity.TrainingSessionStatus;
import com.careertrack.modules.training.repository.TrainingEnrollmentRepository;
import com.careertrack.modules.training.repository.TrainingRepository;
import com.careertrack.modules.training.repository.TrainingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PayslipRepository payslipRepository;

    @Mock
    private TrainingRepository trainingRepository;

    @Mock
    private TrainingSessionRepository trainingSessionRepository;

    @Mock
    private TrainingEnrollmentRepository trainingEnrollmentRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Department techDept;
    private Position devPosition;
    private Employee emp1;
    private Employee emp2;

    @BeforeEach
    void setUp() {
        techDept = Department.builder()
                .id(UUID.randomUUID())
                .name("Technologies")
                .code("TECH")
                .build();

        devPosition = Position.builder()
                .id(UUID.randomUUID())
                .title("Développeur")
                .department(techDept)
                .build();

        emp1 = Employee.builder()
                .id(UUID.randomUUID())
                .firstName("Mamadou")
                .lastName("Diallo")
                .department(techDept)
                .position(devPosition)
                .birthDate(LocalDate.of(1995, 5, 20)) // ~31 ans
                .hireDate(LocalDate.of(2026, 1, 15))
                .status(EmployeeStatus.ACTIVE)
                .build();

        emp2 = Employee.builder()
                .id(UUID.randomUUID())
                .firstName("Fatou")
                .lastName("Sow")
                .department(techDept)
                .position(devPosition)
                .birthDate(LocalDate.of(2003, 3, 10)) // ~23 ans
                .hireDate(LocalDate.of(2025, 6, 1))
                .status(EmployeeStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Génération des statistiques RH globales pour la DRH")
    void shouldGenerateGlobalStatsForDrh() {
        Authentication auth = mock(Authentication.class);
        UserPrincipal principal = UserPrincipal.builder()
                .id(UUID.randomUUID())
                .username("drh")
                .email("drh@careertrack.sn")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_DRH")))
                .build();
        when(auth.getPrincipal()).thenReturn(principal);

        when(employeeRepository.findAll(any(Specification.class))).thenReturn(List.of(emp1, emp2));

        Contract contract1 = Contract.builder()
                .id(UUID.randomUUID())
                .employee(emp1)
                .contractType(ContractType.CDI)
                .active(true)
                .build();
        Contract contract2 = Contract.builder()
                .id(UUID.randomUUID())
                .employee(emp2)
                .contractType(ContractType.CDD)
                .active(true)
                .build();
        when(contractRepository.findByActiveTrue()).thenReturn(List.of(contract1, contract2));

        Payslip payslip1 = Payslip.builder()
                .id(UUID.randomUUID())
                .employee(emp1)
                .periodMonth(1)
                .periodYear(2026)
                .grossSalary(new BigDecimal("1000000"))
                .netSalary(new BigDecimal("750000"))
                .employerContributions(new BigDecimal("150000"))
                .build();
        when(payslipRepository.findByPeriodYearAndFilters(eq(2026), any(), any())).thenReturn(List.of(payslip1));

        when(trainingRepository.count()).thenReturn(5L);
        when(trainingSessionRepository.findAll()).thenReturn(List.of(
                TrainingSession.builder().status(TrainingSessionStatus.IN_PROGRESS).build()
        ));
        when(trainingEnrollmentRepository.count()).thenReturn(12L);

        LeaveRequest leave = LeaveRequest.builder()
                .id(UUID.randomUUID())
                .employee(emp1)
                .startDate(LocalDate.of(2026, 4, 1))
                .requestedDays(new BigDecimal("3.0"))
                .status(LeaveStatus.APPROVED_N1)
                .build();
        when(leaveRequestRepository.findAll()).thenReturn(List.of(leave));

        DashboardStatsDto stats = dashboardService.getDashboardStats(null, null, 2026, auth);

        assertThat(stats).isNotNull();
        assertThat(stats.getPeriodYear()).isEqualTo(2026);

        // Effectifs
        assertThat(stats.getHeadcount().getTotalEmployees()).isEqualTo(2);
        assertThat(stats.getHeadcount().getActiveEmployees()).isEqualTo(2);
        assertThat(stats.getHeadcount().getContractBreakdown()).hasSize(2);

        // Pyramide des âges
        assertThat(stats.getAgePyramid()).hasSize(5);
        var under25 = stats.getAgePyramid().stream().filter(b -> b.getBracket().equals("< 25 ans")).findFirst().orElseThrow();
        assertThat(under25.getCount()).isEqualTo(1);
        var between25and34 = stats.getAgePyramid().stream().filter(b -> b.getBracket().equals("25 - 34 ans")).findFirst().orElseThrow();
        assertThat(between25and34.getCount()).isEqualTo(1);

        // Paie
        assertThat(stats.getPayroll().getTotalGrossSalary()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(stats.getPayroll().getTotalNetSalary()).isEqualByComparingTo(new BigDecimal("750000"));
        assertThat(stats.getPayroll().getMonthlyTrend()).hasSize(12);

        // Formation
        assertThat(stats.getTraining().getTotalTrainings()).isEqualTo(5);
        assertThat(stats.getTraining().getActiveSessions()).isEqualTo(1);

        // Congés
        assertThat(stats.getLeaves().getApprovedRequests()).isEqualTo(1);
        assertThat(stats.getLeaves().getTotalDaysTaken()).isEqualByComparingTo(new BigDecimal("3.0"));
    }

    @Test
    @DisplayName("Scoping automatique au département du manager")
    void shouldScopeToDepartmentForManager() {
        Authentication auth = mock(Authentication.class);
        UUID managerUserId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder()
                .id(managerUserId)
                .username("manager.sow")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_MANAGER")))
                .build();
        when(auth.getPrincipal()).thenReturn(principal);

        Employee managerEmp = Employee.builder()
                .id(UUID.randomUUID())
                .department(techDept)
                .build();
        when(employeeRepository.findByUserId(managerUserId)).thenReturn(Optional.of(managerEmp));

        when(employeeRepository.findAll(any(Specification.class))).thenReturn(List.of(emp1));
        when(contractRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
        when(payslipRepository.findByPeriodYearAndFilters(anyInt(), any(), any())).thenReturn(Collections.emptyList());
        when(trainingSessionRepository.findAll()).thenReturn(Collections.emptyList());
        when(leaveRequestRepository.findAll()).thenReturn(Collections.emptyList());

        DashboardStatsDto stats = dashboardService.getDashboardStats(null, null, 2026, auth);

        assertThat(stats).isNotNull();
        assertThat(stats.getHeadcount().getTotalEmployees()).isEqualTo(1);
    }
}

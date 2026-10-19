package com.careertrack.modules.dashboard.service;

import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.dashboard.dto.*;
import com.careertrack.modules.employee.entity.Contract;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.EmployeeStatus;
import com.careertrack.modules.employee.repository.ContractRepository;
import com.careertrack.modules.employee.repository.DepartmentRepository;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.employee.repository.EmployeeSpecification;
import com.careertrack.modules.leave.entity.LeaveRequest;
import com.careertrack.modules.leave.repository.LeaveRequestRepository;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.repository.PayslipRepository;
import com.careertrack.modules.training.entity.TrainingSession;
import com.careertrack.modules.training.entity.TrainingSessionStatus;
import com.careertrack.modules.training.repository.TrainingEnrollmentRepository;
import com.careertrack.modules.training.repository.TrainingRepository;
import com.careertrack.modules.training.repository.TrainingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final String[] MONTH_NAMES = {
            "Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc"
    };

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ContractRepository contractRepository;
    private final PayslipRepository payslipRepository;
    private final TrainingRepository trainingRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainingEnrollmentRepository trainingEnrollmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(UUID departmentId, UUID positionId, Integer periodYear, Authentication authentication) {
        int year = (periodYear != null) ? periodYear : LocalDate.now().getYear();

        // 1. Scoping automatique selon le rôle
        UUID effectiveDeptId = departmentId;
        UUID managerEmployeeId = null;

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            boolean isDrhOrDir = principal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_DRH") || a.getAuthority().equals("ROLE_DIRECTION"));

            if (!isDrhOrDir) {
                // Rôle MANAGER : restreindre à son département et ses collaborateurs
                Employee currentEmp = employeeRepository.findByUserId(principal.getId()).orElse(null);
                if (currentEmp != null) {
                    managerEmployeeId = currentEmp.getId();
                    if (currentEmp.getDepartment() != null) {
                        effectiveDeptId = currentEmp.getDepartment().getId();
                    }
                }
            }
        }

        // 2. Récupérer les employés concernés
        Specification<Employee> spec = EmployeeSpecification.withDashboardFilters(effectiveDeptId, positionId, managerEmployeeId);
        List<Employee> employees = employeeRepository.findAll(spec);
        Set<UUID> employeeIds = employees.stream().map(Employee::getId).collect(Collectors.toSet());

        // 3. Effectifs et Contrats
        HeadcountKpiDto headcount = computeHeadcountKpis(employees, employeeIds, effectiveDeptId);

        // 4. Pyramide des âges
        List<AgePyramidBracketDto> agePyramid = computeAgePyramid(employees);

        // 5. Taux de turnover
        TurnoverKpiDto turnover = computeTurnover(employees, year, headcount.getActiveEmployees());

        // 6. Masse salariale
        PayrollKpiDto payroll = computePayrollKpis(year, effectiveDeptId, positionId, employeeIds);

        // 7. Plan de formation
        TrainingKpiDto training = computeTrainingKpis();

        // 8. Indicateurs congés
        LeaveKpiDto leaves = computeLeaveKpis(year, employeeIds);

        return DashboardStatsDto.builder()
                .periodYear(year)
                .headcount(headcount)
                .agePyramid(agePyramid)
                .turnover(turnover)
                .payroll(payroll)
                .training(training)
                .leaves(leaves)
                .build();
    }

    private HeadcountKpiDto computeHeadcountKpis(List<Employee> employees, Set<UUID> employeeIds, UUID departmentId) {
        long total = employees.size();
        long active = employees.stream().filter(e -> e.getStatus() == EmployeeStatus.ACTIVE).count();
        long onLeave = employees.stream().filter(e -> e.getStatus() == EmployeeStatus.ON_LEAVE).count();
        long terminated = employees.stream().filter(e -> e.getStatus() == EmployeeStatus.TERMINATED).count();

        // Répartition par type de contrat (parmi les employés actifs)
        List<Contract> activeContracts = contractRepository.findByActiveTrue().stream()
                .filter(c -> c.getEmployee() != null && employeeIds.contains(c.getEmployee().getId()))
                .toList();

        Map<String, Long> contractCounts = activeContracts.stream()
                .collect(Collectors.groupingBy(c -> c.getContractType().name(), Collectors.counting()));

        long totalContracts = activeContracts.size();
        List<ContractTypeBreakdownDto> contractBreakdown = new ArrayList<>();
        contractCounts.forEach((type, count) -> {
            double pct = (totalContracts > 0) ? Math.round(((double) count / totalContracts * 100.0) * 10.0) / 10.0 : 0.0;
            contractBreakdown.add(ContractTypeBreakdownDto.builder()
                    .contractType(type)
                    .count(count)
                    .percentage(pct)
                    .build());
        });

        // Répartition par département (si vue globale)
        List<DepartmentHeadcountDto> departmentBreakdown = new ArrayList<>();
        if (departmentId == null) {
            Map<String, List<Employee>> byDept = employees.stream()
                    .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE && e.getDepartment() != null)
                    .collect(Collectors.groupingBy(e -> e.getDepartment().getName()));

            byDept.forEach((deptName, list) -> {
                UUID deptId = list.get(0).getDepartment().getId();
                departmentBreakdown.add(DepartmentHeadcountDto.builder()
                        .departmentId(deptId)
                        .departmentName(deptName)
                        .employeeCount(list.size())
                        .build());
            });
        }

        return HeadcountKpiDto.builder()
                .totalEmployees(total)
                .activeEmployees(active)
                .onLeaveEmployees(onLeave)
                .terminatedEmployees(terminated)
                .contractBreakdown(contractBreakdown)
                .departmentBreakdown(departmentBreakdown)
                .build();
    }

    private List<AgePyramidBracketDto> computeAgePyramid(List<Employee> employees) {
        List<Employee> activeEmployees = employees.stream()
                .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE && e.getBirthDate() != null)
                .toList();

        long under25 = 0;
        long between25and34 = 0;
        long between35and44 = 0;
        long between45and54 = 0;
        long over55 = 0;

        LocalDate now = LocalDate.now();
        for (Employee e : activeEmployees) {
            int age = Period.between(e.getBirthDate(), now).getYears();
            if (age < 25) under25++;
            else if (age <= 34) between25and34++;
            else if (age <= 44) between35and44++;
            else if (age <= 54) between45and54++;
            else over55++;
        }

        long total = activeEmployees.size();
        List<AgePyramidBracketDto> pyramid = new ArrayList<>();
        pyramid.add(buildBracket("< 25 ans", under25, total));
        pyramid.add(buildBracket("25 - 34 ans", between25and34, total));
        pyramid.add(buildBracket("35 - 44 ans", between35and44, total));
        pyramid.add(buildBracket("45 - 54 ans", between45and54, total));
        pyramid.add(buildBracket("55+ ans", over55, total));

        return pyramid;
    }

    private AgePyramidBracketDto buildBracket(String label, long count, long total) {
        double pct = (total > 0) ? Math.round(((double) count / total * 100.0) * 10.0) / 10.0 : 0.0;
        return AgePyramidBracketDto.builder()
                .bracket(label)
                .count(count)
                .percentage(pct)
                .build();
    }

    private TurnoverKpiDto computeTurnover(List<Employee> employees, int year, long activeCount) {
        long hires = employees.stream()
                .filter(e -> e.getHireDate() != null && e.getHireDate().getYear() == year)
                .count();

        long departures = employees.stream()
                .filter(e -> e.getStatus() == EmployeeStatus.TERMINATED)
                .count();

        long avgHeadcount = Math.max(1, activeCount);
        double rate = Math.round(((double) (hires + departures) / (2.0 * avgHeadcount) * 100.0) * 10.0) / 10.0;

        return TurnoverKpiDto.builder()
                .hiresCount(hires)
                .departuresCount(departures)
                .averageHeadcount(avgHeadcount)
                .turnoverRate(rate)
                .build();
    }

    private PayrollKpiDto computePayrollKpis(int year, UUID departmentId, UUID positionId, Set<UUID> employeeIds) {
        List<Payslip> payslips = payslipRepository.findByPeriodYearAndFilters(year, departmentId, positionId).stream()
                .filter(p -> p.getEmployee() != null && employeeIds.contains(p.getEmployee().getId()))
                .toList();

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalEmployerCharges = BigDecimal.ZERO;

        Map<Integer, List<Payslip>> byMonth = payslips.stream()
                .collect(Collectors.groupingBy(Payslip::getPeriodMonth));

        List<MonthlyPayrollTrendDto> monthlyTrend = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            List<Payslip> monthSlips = byMonth.getOrDefault(m, Collections.emptyList());
            BigDecimal mGross = monthSlips.stream().map(Payslip::getGrossSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mNet = monthSlips.stream().map(Payslip::getNetSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mCharges = monthSlips.stream().map(Payslip::getEmployerContributions).reduce(BigDecimal.ZERO, BigDecimal::add);

            totalGross = totalGross.add(mGross);
            totalNet = totalNet.add(mNet);
            totalEmployerCharges = totalEmployerCharges.add(mCharges);

            monthlyTrend.add(MonthlyPayrollTrendDto.builder()
                    .month(m)
                    .monthLabel(MONTH_NAMES[m - 1])
                    .grossSalary(mGross)
                    .netSalary(mNet)
                    .employerContributions(mCharges)
                    .build());
        }

        return PayrollKpiDto.builder()
                .totalGrossSalary(totalGross)
                .totalNetSalary(totalNet)
                .totalEmployerContributions(totalEmployerCharges)
                .payslipsCount(payslips.size())
                .monthlyTrend(monthlyTrend)
                .build();
    }

    private TrainingKpiDto computeTrainingKpis() {
        long totalTrainings = trainingRepository.count();
        List<TrainingSession> sessions = trainingSessionRepository.findAll();

        long activeSessions = sessions.stream()
                .filter(s -> s.getStatus() == TrainingSessionStatus.SCHEDULED || s.getStatus() == TrainingSessionStatus.IN_PROGRESS)
                .count();

        long completedSessions = sessions.stream()
                .filter(s -> s.getStatus() == TrainingSessionStatus.COMPLETED)
                .count();

        long totalParticipants = trainingEnrollmentRepository.count();

        return TrainingKpiDto.builder()
                .totalTrainings(totalTrainings)
                .activeSessions(activeSessions)
                .completedSessions(completedSessions)
                .totalParticipants(totalParticipants)
                .build();
    }

    private LeaveKpiDto computeLeaveKpis(int year, Set<UUID> employeeIds) {
        List<LeaveRequest> employeeRequests = leaveRequestRepository.findAll().stream()
                .filter(lr -> lr.getEmployee() != null && employeeIds.contains(lr.getEmployee().getId()))
                .toList();

        long pending = employeeRequests.stream()
                .filter(lr -> lr.getStatus() != null && lr.getStatus().isPending())
                .count();

        List<LeaveRequest> yearlyRequests = employeeRequests.stream()
                .filter(lr -> lr.getStartDate() == null || lr.getStartDate().getYear() == year)
                .toList();

        long approved = yearlyRequests.stream().filter(lr -> lr.getStatus() != null && lr.getStatus().isApproved()).count();
        long rejected = yearlyRequests.stream().filter(lr -> lr.getStatus() != null && lr.getStatus().isRejected()).count();

        BigDecimal totalDaysTaken = yearlyRequests.stream()
                .filter(lr -> lr.getStatus() != null && lr.getStatus().isApproved() && lr.getRequestedDays() != null)
                .map(LeaveRequest::getRequestedDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return LeaveKpiDto.builder()
                .pendingRequests(pending)
                .approvedRequests(approved)
                .rejectedRequests(rejected)
                .totalDaysTaken(totalDaysTaken)
                .build();
    }
}

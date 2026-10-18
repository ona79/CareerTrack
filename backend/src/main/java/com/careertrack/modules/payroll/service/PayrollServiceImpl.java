package com.careertrack.modules.payroll.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.employee.entity.Contract;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.EmployeeStatus;
import com.careertrack.modules.employee.repository.ContractRepository;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.payroll.dto.*;
import com.careertrack.modules.payroll.entity.*;
import com.careertrack.modules.payroll.repository.PayrollParameterRepository;
import com.careertrack.modules.payroll.repository.PayrollTaxBracketRepository;
import com.careertrack.modules.payroll.repository.PayrollTrimfBracketRepository;
import com.careertrack.modules.payroll.repository.PayslipLineRepository;
import com.careertrack.modules.payroll.repository.PayslipRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PayrollServiceImpl implements PayrollService {

    private final PayslipRepository payslipRepository;
    private final PayslipLineRepository payslipLineRepository;
    private final EmployeeRepository employeeRepository;
    private final ContractRepository contractRepository;
    private final PayrollParameterRepository payrollParameterRepository;
    private final PayrollTaxBracketRepository payrollTaxBracketRepository;
    private final PayrollTrimfBracketRepository payrollTrimfBracketRepository;
    private final PayrollCalculator payrollCalculator;

    public PayrollServiceImpl(
            PayslipRepository payslipRepository,
            PayslipLineRepository payslipLineRepository,
            EmployeeRepository employeeRepository,
            ContractRepository contractRepository,
            PayrollParameterRepository payrollParameterRepository,
            PayrollTaxBracketRepository payrollTaxBracketRepository,
            PayrollTrimfBracketRepository payrollTrimfBracketRepository,
            PayrollCalculator payrollCalculator
    ) {
        this.payslipRepository = payslipRepository;
        this.payslipLineRepository = payslipLineRepository;
        this.employeeRepository = employeeRepository;
        this.contractRepository = contractRepository;
        this.payrollParameterRepository = payrollParameterRepository;
        this.payrollTaxBracketRepository = payrollTaxBracketRepository;
        this.payrollTrimfBracketRepository = payrollTrimfBracketRepository;
        this.payrollCalculator = payrollCalculator;
    }

    @Override
    public List<PayslipResponseDto> getPayslipsByPeriod(String period) {
        YearMonth ym = parsePeriod(period);
        return payslipRepository.findByPeriodYearAndPeriodMonth(ym.getYear(), ym.getMonthValue()).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public List<PayslipResponseDto> getPayslipsByEmployee(UUID employeeId) {
        return payslipRepository.findByEmployeeIdOrderByPeriodYearDescPeriodMonthDesc(employeeId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public PayslipResponseDto getPayslipById(UUID payslipId) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Bulletin de paie introuvable : " + payslipId));
        return mapToDto(payslip);
    }

    @Override
    @Transactional
    public PayrollGenerationResultDto generatePayrollForPeriod(String period) {
        YearMonth ym = parsePeriod(period);
        LocalDate effectiveDate = ym.atEndOfMonth();

        // 1. Récupération des paramètres valides à la date d'effet (dernier jour du mois)
        List<PayrollParameter> allParams = payrollParameterRepository.findAllValidAt(effectiveDate);
        Map<String, String> paramMap = new HashMap<>();
        for (PayrollParameter p : allParams) {
            paramMap.putIfAbsent(p.getKey(), p.getValue());
        }

        List<PayrollTaxBracket> taxBrackets = payrollTaxBracketRepository.findActiveBracketsAt(effectiveDate);
        List<PayrollTrimfBracket> trimfBrackets = payrollTrimfBracketRepository.findActiveBracketsAt(effectiveDate);

        // 2. Récupération des collaborateurs actifs
        List<Employee> activeEmployees = employeeRepository.findAll().stream()
                .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE)
                .toList();

        int totalProcessed = 0;
        int totalGenerated = 0;
        int totalSkipped = 0;
        List<PayslipResponseDto> generatedDtos = new ArrayList<>();

        for (Employee emp : activeEmployees) {
            totalProcessed++;

            // Seuls les collaborateurs avec un contrat actif sont pris en compte
            Optional<Contract> activeContractOpt = contractRepository.findByEmployeeIdAndActiveTrue(emp.getId());
            if (activeContractOpt.isEmpty()) {
                totalSkipped++;
                continue;
            }

            Contract contract = activeContractOpt.get();
            BigDecimal grossSalary = contract.getGrossSalary();
            if (grossSalary == null || grossSalary.compareTo(BigDecimal.ZERO) <= 0) {
                totalSkipped++;
                continue;
            }

            // Vérification d'un bulletin existant pour la période
            Optional<Payslip> existingOpt = payslipRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(
                    emp.getId(), ym.getYear(), ym.getMonthValue());

            if (existingOpt.isPresent()) {
                Payslip existing = existingOpt.get();
                // Si le bulletin est validé ou payé, il est intouchable (idempotence stricte)
                if (existing.getStatus() == PayslipStatus.VALIDATED || existing.getStatus() == PayslipStatus.PAID) {
                    totalSkipped++;
                    generatedDtos.add(mapToDto(existing));
                    continue;
                }
            }

            // Calcul de paie via le moteur pur
            PayrollCalculator.CalculationResult calcResult = payrollCalculator.calculate(
                    grossSalary,
                    emp.isCadre(),
                    emp.getMaritalStatus(),
                    emp.getDependentsCount(),
                    paramMap,
                    taxBrackets,
                    trimfBrackets
            );

            // Création ou mise à jour du bulletin (GENERATED ou CANCELLED -> GENERATED)
            Payslip payslip = existingOpt.orElseGet(() -> Payslip.builder()
                    .employee(emp)
                    .periodYear(ym.getYear())
                    .periodMonth(ym.getMonthValue())
                    .build());

            payslip.setGrossSalary(calcResult.grossSalary());
            payslip.setTaxableGross(calcResult.taxableGross());
            payslip.setTaxParts(calcResult.taxParts());
            payslip.setTotalDeductions(calcResult.totalDeductions());
            payslip.setNetSalary(calcResult.netSalary());
            payslip.setEmployerContributions(calcResult.totalEmployerContributions());
            payslip.setPaymentDate(effectiveDate);
            payslip.setStatus(PayslipStatus.GENERATED);
            payslip.setCancelledBy(null);
            payslip.setCancelledAt(null);

            Payslip savedPayslip = payslipRepository.save(payslip);

            // Remplacement des lignes de paie
            List<PayslipLine> oldLines = payslipLineRepository.findByPayslipId(savedPayslip.getId());
            if (!oldLines.isEmpty()) {
                payslipLineRepository.deleteAll(oldLines);
            }

            List<PayslipLine> newLines = calcResult.lines().stream()
                    .map(l -> PayslipLine.builder()
                            .payslip(savedPayslip)
                            .lineType(l.lineType())
                            .category(l.category())
                            .label(l.label())
                            .baseAmount(l.baseAmount())
                            .rate(l.rate())
                            .amount(l.amount())
                            .build())
                    .toList();

            payslipLineRepository.saveAll(newLines);

            totalGenerated++;
            generatedDtos.add(mapToDto(savedPayslip));
        }

        BigDecimal totalGross = generatedDtos.stream().map(PayslipResponseDto::getGrossSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = generatedDtos.stream().map(PayslipResponseDto::getNetSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEmployer = generatedDtos.stream().map(PayslipResponseDto::getEmployerContributions).reduce(BigDecimal.ZERO, BigDecimal::add);

        return PayrollGenerationResultDto.builder()
                .period(period)
                .totalProcessed(totalProcessed)
                .totalGenerated(totalGenerated)
                .totalSkipped(totalSkipped)
                .totalGross(totalGross)
                .totalNet(totalNet)
                .totalEmployerContributions(totalEmployer)
                .payslips(generatedDtos)
                .build();
    }

    @Override
    @Transactional
    public PayslipResponseDto updateStatus(UUID payslipId, PayslipStatus newStatus) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Bulletin introuvable : " + payslipId));

        PayslipStatus current = payslip.getStatus();

        if (current == PayslipStatus.PAID) {
            throw new IllegalStateException("Un bulletin au statut PAYÉ est immuable et ne peut plus être modifié.");
        }

        if (newStatus == PayslipStatus.CANCELLED) {
            if (current != PayslipStatus.VALIDATED && current != PayslipStatus.GENERATED) {
                throw new IllegalStateException("Seul un bulletin généré ou validé peut être annulé.");
            }
            payslip.setStatus(PayslipStatus.CANCELLED);
            payslip.setCancelledBy(getCurrentUsername());
            payslip.setCancelledAt(Instant.now());
        } else if (newStatus == PayslipStatus.VALIDATED) {
            if (current != PayslipStatus.GENERATED && current != PayslipStatus.DRAFT) {
                throw new IllegalStateException("Transition invalide vers VALIDATED depuis " + current);
            }
            payslip.setStatus(PayslipStatus.VALIDATED);
        } else if (newStatus == PayslipStatus.PAID) {
            if (current != PayslipStatus.VALIDATED) {
                throw new IllegalStateException("Un bulletin doit d'abord être validé avant d'être payé.");
            }
            payslip.setStatus(PayslipStatus.PAID);
        } else {
            payslip.setStatus(newStatus);
        }

        return mapToDto(payslipRepository.save(payslip));
    }

    @Override
    public PayrollStatsResponseDto getPayrollStats(String period) {
        YearMonth ym = parsePeriod(period);
        List<Payslip> payslips = payslipRepository.findByPeriodYearAndPeriodMonth(ym.getYear(), ym.getMonthValue()).stream()
                .filter(p -> p.getStatus() != PayslipStatus.CANCELLED)
                .toList();

        int totalEmployees = payslips.size();
        BigDecimal totalGross = payslips.stream().map(Payslip::getGrossSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = payslips.stream().map(Payslip::getNetSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDeductions = payslips.stream().map(Payslip::getTotalDeductions).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEmployerContributions = payslips.stream().map(Payslip::getEmployerContributions).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEmployerCost = totalGross.add(totalEmployerContributions);

        // Règle de confidentialité : si effectif total < 3, aucune ventilation par département
        if (totalEmployees < 3) {
            return PayrollStatsResponseDto.builder()
                    .period(period)
                    .totalEmployeesPaid(totalEmployees)
                    .totalGrossSalary(totalGross)
                    .totalNetSalary(totalNet)
                    .totalDeductions(totalDeductions)
                    .totalEmployerContributions(totalEmployerContributions)
                    .totalEmployerCost(totalEmployerCost)
                    .departmentBreakdown(Collections.emptyList())
                    .build();
        }

        // Groupement par département
        Map<String, List<Payslip>> byDept = payslips.stream()
                .collect(Collectors.groupingBy(p -> {
                    if (p.getEmployee().getDepartment() != null && p.getEmployee().getDepartment().getName() != null) {
                        return p.getEmployee().getDepartment().getName();
                    }
                    return "Non affecté";
                }));

        List<DepartmentPayrollStatsDto> breakdown = new ArrayList<>();
        int othersCount = 0;
        BigDecimal othersGross = BigDecimal.ZERO;
        BigDecimal othersNet = BigDecimal.ZERO;
        BigDecimal othersEmployerCost = BigDecimal.ZERO;

        for (Map.Entry<String, List<Payslip>> entry : byDept.entrySet()) {
            String deptName = entry.getKey();
            List<Payslip> deptPayslips = entry.getValue();
            int count = deptPayslips.size();

            BigDecimal deptGross = deptPayslips.stream().map(Payslip::getGrossSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal deptNet = deptPayslips.stream().map(Payslip::getNetSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal deptEmployerContrib = deptPayslips.stream().map(Payslip::getEmployerContributions).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal deptCost = deptGross.add(deptEmployerContrib);

            if (count >= 3) {
                breakdown.add(DepartmentPayrollStatsDto.builder()
                        .departmentName(deptName)
                        .employeeCount(count)
                        .grossSalary(deptGross)
                        .netSalary(deptNet)
                        .employerCost(deptCost)
                        .build());
            } else {
                // Regroupement dans "Autres" pour préserver la confidentialité salariale
                othersCount += count;
                othersGross = othersGross.add(deptGross);
                othersNet = othersNet.add(deptNet);
                othersEmployerCost = othersEmployerCost.add(deptCost);
            }
        }

        // Si "Autres" compte lui-même >= 3 salariés, on l'affiche ; sinon, masqué pour anonymat
        if (othersCount >= 3) {
            breakdown.add(DepartmentPayrollStatsDto.builder()
                    .departmentName("Autres")
                    .employeeCount(othersCount)
                    .grossSalary(othersGross)
                    .netSalary(othersNet)
                    .employerCost(othersEmployerCost)
                    .build());
        }

        breakdown.sort(Comparator.comparing(DepartmentPayrollStatsDto::getDepartmentName));

        return PayrollStatsResponseDto.builder()
                .period(period)
                .totalEmployeesPaid(totalEmployees)
                .totalGrossSalary(totalGross)
                .totalNetSalary(totalNet)
                .totalDeductions(totalDeductions)
                .totalEmployerContributions(totalEmployerContributions)
                .totalEmployerCost(totalEmployerCost)
                .departmentBreakdown(breakdown)
                .build();
    }

    @Override
    public List<PayrollParameterResponseDto> getPayrollParameters() {
        return payrollParameterRepository.findAllByOrderByKeyAsc().stream()
                .map(p -> PayrollParameterResponseDto.builder()
                        .id(p.getId())
                        .key(p.getKey())
                        .value(p.getValue())
                        .description(p.getDescription())
                        .category(p.getCategory())
                        .status(p.getStatus())
                        .source(p.getSource())
                        .validFrom(p.getValidFrom())
                        .build())
                .toList();
    }

    private YearMonth parsePeriod(String period) {
        if (period == null || !period.matches("^\\d{4}-\\d{2}$")) {
            throw new IllegalArgumentException("Format de période invalide. Format attendu : YYYY-MM (ex: 2026-09)");
        }
        return YearMonth.parse(period);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            if (auth.getPrincipal() instanceof UserPrincipal principal) {
                return principal.getUsername();
            }
            return auth.getName();
        }
        return "SYSTEM";
    }

    private PayslipResponseDto mapToDto(Payslip payslip) {
        List<PayslipLine> lines = payslipLineRepository.findByPayslipId(payslip.getId());
        List<PayslipResponseDto.PayslipLineDto> lineDtos = lines.stream()
                .map(l -> PayslipResponseDto.PayslipLineDto.builder()
                        .id(l.getId())
                        .lineType(l.getLineType())
                        .category(l.getCategory())
                        .label(l.getLabel())
                        .baseAmount(l.getBaseAmount())
                        .rate(l.getRate())
                        .amount(l.getAmount())
                        .build())
                .toList();

        String period = String.format("%04d-%02d", payslip.getPeriodYear(), payslip.getPeriodMonth());

        return PayslipResponseDto.builder()
                .id(payslip.getId())
                .employeeId(payslip.getEmployee().getId())
                .employeeFullName(payslip.getEmployee().getFullName())
                .employeeNumber(payslip.getEmployee().getEmployeeNumber())
                .periodMonth(payslip.getPeriodMonth())
                .periodYear(payslip.getPeriodYear())
                .period(period)
                .grossSalary(payslip.getGrossSalary())
                .taxableGross(payslip.getTaxableGross())
                .taxParts(payslip.getTaxParts())
                .totalDeductions(payslip.getTotalDeductions())
                .netSalary(payslip.getNetSalary())
                .employerContributions(payslip.getEmployerContributions())
                .status(payslip.getStatus())
                .paymentDate(payslip.getPaymentDate())
                .cancelledBy(payslip.getCancelledBy())
                .cancelledAt(payslip.getCancelledAt())
                .pdfUrl(payslip.getPdfUrl())
                .lines(lineDtos)
                .build();
    }
}

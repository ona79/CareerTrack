package com.careertrack.modules.payroll.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.entity.Contract;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.repository.ContractRepository;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.payroll.dto.PayslipResponseDto;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.entity.PayslipLine;
import com.careertrack.modules.payroll.entity.PayslipLineType;
import com.careertrack.modules.payroll.entity.PayslipStatus;
import com.careertrack.modules.payroll.repository.PayslipLineRepository;
import com.careertrack.modules.payroll.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollServiceImpl implements PayrollService {

    private final PayslipRepository payslipRepository;
    private final PayslipLineRepository payslipLineRepository;
    private final EmployeeRepository employeeRepository;
    private final ContractRepository contractRepository;

    @Value("${careertrack.labor-rules.ipres-rate-employee:0.056}")
    private BigDecimal ipresEmployeeRate;

    @Value("${careertrack.labor-rules.ipres-rate-employer:0.084}")
    private BigDecimal ipresEmployerRate;

    @Value("${careertrack.labor-rules.css-rate-employer:0.070}")
    private BigDecimal cssEmployerRate;

    @Value("${careertrack.labor-rules.default-ir-rate-estimate:0.12}")
    private BigDecimal irEstimateRate;

    @Value("${careertrack.labor-rules.trimf-monthly-amount:900}")
    private BigDecimal trimfMonthlyAmount;

    @Override
    public List<PayslipResponseDto> getPayslipsByPeriod(String period) {
        int[] ym = parsePeriod(period);
        return payslipRepository.findByPeriodYearAndPeriodMonth(ym[0], ym[1]).stream()
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
    public Payslip generateMonthlyPayslip(UUID employeeId, String period) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + employeeId));

        int[] ym = parsePeriod(period);
        int year = ym[0];
        int month = ym[1];

        BigDecimal baseSalary = contractRepository.findByEmployeeIdAndActiveTrue(employeeId)
                .map(Contract::getGrossSalary)
                .orElse(BigDecimal.valueOf(300000)); // Repli

        // Calculs des cotisations sénégalaises
        BigDecimal ipresSalarial = baseSalary.multiply(ipresEmployeeRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal irRetenue = baseSalary.multiply(irEstimateRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal trimf = trimfMonthlyAmount;

        BigDecimal totalDeductions = ipresSalarial.add(irRetenue).add(trimf);
        BigDecimal netSalary = baseSalary.subtract(totalDeductions);

        // Cotisations patronales (CSS 7% + IPRES patronal 8,4%)
        BigDecimal ipresPatronal = baseSalary.multiply(ipresEmployerRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal cssPatronal = baseSalary.multiply(cssEmployerRate).setScale(0, RoundingMode.HALF_UP);

        Payslip payslip = payslipRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(employeeId, year, month)
                .orElseGet(() -> Payslip.builder()
                        .employee(employee)
                        .periodYear(year)
                        .periodMonth(month)
                        .paymentDate(LocalDate.of(year, month, 28))
                        .build());

        payslip.setGrossSalary(baseSalary);
        payslip.setTotalDeductions(totalDeductions);
        payslip.setNetSalary(netSalary);
        payslip.setStatus(PayslipStatus.GENERATED);

        Payslip saved = payslipRepository.save(payslip);

        // Supprimer anciennes lignes
        List<PayslipLine> oldLines = payslipLineRepository.findByPayslipId(saved.getId());
        payslipLineRepository.deleteAll(oldLines);

        // Nouvelles lignes de paie avec les types exacts de l'énumération sénégalaise
        List<PayslipLine> lines = new ArrayList<>();
        lines.add(PayslipLine.builder().payslip(saved).lineType(PayslipLineType.BASE_SALARY).label("Salaire de base brut").baseAmount(baseSalary).rate(BigDecimal.ONE).amount(baseSalary).build());
        lines.add(PayslipLine.builder().payslip(saved).lineType(PayslipLineType.IPRES_EMPLOYEE).label("Retraite IPRES Salariale (5.6%)").baseAmount(baseSalary).rate(ipresEmployeeRate).amount(ipresSalarial.negate()).build());
        lines.add(PayslipLine.builder().payslip(saved).lineType(PayslipLineType.TAX_IR).label("Retenue Impôt sur le Revenu (IR estimé)").baseAmount(baseSalary).rate(irEstimateRate).amount(irRetenue.negate()).build());
        lines.add(PayslipLine.builder().payslip(saved).lineType(PayslipLineType.OTHER_DEDUCTION).label("TRIMF (Taxe Représentative Minimum Fiscal)").baseAmount(BigDecimal.ZERO).rate(BigDecimal.ZERO).amount(trimf.negate()).build());
        lines.add(PayslipLine.builder().payslip(saved).lineType(PayslipLineType.IPRES_EMPLOYER).label("IPRES Part Patronale (8.4%)").baseAmount(baseSalary).rate(ipresEmployerRate).amount(ipresPatronal).build());
        lines.add(PayslipLine.builder().payslip(saved).lineType(PayslipLineType.CSS_EMPLOYER).label("CSS Prestations Familiales & AT/MP (7.0%)").baseAmount(baseSalary).rate(cssEmployerRate).amount(cssPatronal).build());

        payslipLineRepository.saveAll(lines);
        return saved;
    }

    @Override
    @Transactional
    public List<PayslipResponseDto> generatePayrollForPeriod(String period) {
        List<Employee> activeEmployees = employeeRepository.findAll();
        List<PayslipResponseDto> result = new ArrayList<>();
        for (Employee emp : activeEmployees) {
            Payslip p = generateMonthlyPayslip(emp.getId(), period);
            result.add(mapToDto(p));
        }
        return result;
    }

    @Override
    @Transactional
    public PayslipResponseDto updateStatus(UUID payslipId, PayslipStatus newStatus) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Bulletin introuvable : " + payslipId));
        payslip.setStatus(newStatus);
        return mapToDto(payslipRepository.save(payslip));
    }

    private int[] parsePeriod(String period) {
        String[] parts = period.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        return new int[]{year, month};
    }

    private PayslipResponseDto mapToDto(Payslip payslip) {
        List<PayslipLine> lines = payslipLineRepository.findByPayslipId(payslip.getId());
        List<PayslipResponseDto.PayslipLineDto> lineDtos = lines.stream()
                .map(l -> PayslipResponseDto.PayslipLineDto.builder()
                        .id(l.getId())
                        .lineType(l.getLineType())
                        .label(l.getLabel())
                        .baseAmount(l.getBaseAmount())
                        .rate(l.getRate())
                        .amount(l.getAmount())
                        .build())
                .toList();

        BigDecimal employerPart = lines.stream()
                .filter(l -> l.getLineType() == PayslipLineType.IPRES_EMPLOYER || l.getLineType() == PayslipLineType.CSS_EMPLOYER)
                .map(PayslipLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
                .totalDeductions(payslip.getTotalDeductions())
                .netSalary(payslip.getNetSalary())
                .employerContributions(employerPart)
                .status(payslip.getStatus())
                .paymentDate(payslip.getPaymentDate())
                .pdfUrl(payslip.getPdfUrl())
                .lines(lineDtos)
                .build();
    }
}

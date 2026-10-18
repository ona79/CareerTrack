package com.careertrack.modules.document.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.common.pdf.PdfGeneratorService;
import com.careertrack.modules.employee.entity.Contract;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.repository.ContractRepository;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentServiceImpl implements DocumentService {

    private final EmployeeRepository employeeRepository;
    private final ContractRepository contractRepository;
    private final PayslipRepository payslipRepository;
    private final PdfGeneratorService pdfGeneratorService;

    @Override
    public byte[] generateWorkCertificate(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable avec l'identifiant : " + employeeId));

        Contract contract = contractRepository.findByEmployeeIdAndActiveTrue(employeeId)
                .orElse(null);

        return pdfGeneratorService.generateWorkCertificate(employee, contract);
    }

    @Override
    public String getWorkCertificateFilename(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable avec l'identifiant : " + employeeId));
        return "attestation-travail-" + employee.getEmployeeNumber() + ".pdf";
    }

    @Override
    public byte[] generatePayslip(UUID payslipId) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Bulletin de paie introuvable avec l'identifiant : " + payslipId));

        return pdfGeneratorService.generatePayslipPdf(payslip);
    }

    @Override
    public String getPayslipFilename(UUID payslipId) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Bulletin de paie introuvable avec l'identifiant : " + payslipId));
        return "bulletin-paie-" + payslip.getPeriodYear() + "-" + String.format("%02d", payslip.getPeriodMonth()) + "-" + payslip.getEmployee().getEmployeeNumber() + ".pdf";
    }

    @Override
    public byte[] generateEmploymentContract(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable avec l'identifiant : " + employeeId));

        Contract contract = contractRepository.findByEmployeeIdAndActiveTrue(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun contrat actif trouvé pour l'employé : " + employee.getEmployeeNumber()));

        return pdfGeneratorService.generateEmploymentContract(employee, contract);
    }

    @Override
    public String getContractFilename(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable avec l'identifiant : " + employeeId));
        return "contrat-travail-" + employee.getEmployeeNumber() + ".pdf";
    }
}

package com.careertrack.modules.document.service;

import java.util.UUID;

public interface DocumentService {
    byte[] generateWorkCertificate(UUID employeeId);
    String getWorkCertificateFilename(UUID employeeId);

    byte[] generatePayslip(UUID payslipId);
    String getPayslipFilename(UUID payslipId);

    byte[] generateEmploymentContract(UUID employeeId);
    String getContractFilename(UUID employeeId);
}

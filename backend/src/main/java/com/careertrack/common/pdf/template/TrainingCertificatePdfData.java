package com.careertrack.common.pdf.template;

import java.time.LocalDate;

/**
 * Objet de transfert pour le gabarit PDF d'attestation de formation (Lot 4).
 */
public record TrainingCertificatePdfData(
        String certificateNumber,
        String employeeName,
        String employeeNumber,
        String positionTitle,
        String trainingTitle,
        String organizationName,
        LocalDate startDate,
        LocalDate completionDate,
        int durationHours,
        String skillsAcquired
) {
}

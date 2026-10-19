package com.careertrack.modules.training.dto;

import com.careertrack.modules.training.entity.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingEnrollmentDto {
    private UUID id;
    private UUID sessionId;
    private String sessionCode;
    private UUID trainingId;
    private String trainingTitle;
    private Integer durationHours;
    private String provider;
    private Instant sessionStartDate;
    private Instant sessionEndDate;
    private UUID employeeId;
    private String employeeFullName;
    private String employeeNumber;
    private LocalDate enrollmentDate;
    private EnrollmentStatus status;
    private BigDecimal score;
    private String certificateUrl;
    private String feedback;
}

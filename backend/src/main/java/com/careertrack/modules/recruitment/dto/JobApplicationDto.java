package com.careertrack.modules.recruitment.dto;

import com.careertrack.modules.recruitment.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationDto {
    private UUID id;
    private UUID jobOfferId;
    private String jobOfferTitle;
    private UUID candidateId;
    private String candidateFullName;
    private String candidateEmail;
    private String candidatePhone;
    private ApplicationStatus status;
    private String currentStep;
    private int rating;
    private String notes;
    private String cvUrl;
    private String appliedAt;
}

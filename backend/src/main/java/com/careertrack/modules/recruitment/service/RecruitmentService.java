package com.careertrack.modules.recruitment.service;

import com.careertrack.modules.recruitment.dto.JobApplicationCreateDto;
import com.careertrack.modules.recruitment.dto.JobApplicationDto;
import com.careertrack.modules.recruitment.dto.JobOfferCreateDto;
import com.careertrack.modules.recruitment.dto.JobOfferDto;
import com.careertrack.modules.recruitment.entity.ApplicationStatus;
import com.careertrack.modules.recruitment.entity.JobOffer;

import java.util.List;
import java.util.UUID;

public interface RecruitmentService {
    List<JobApplicationDto> getAllApplications();
    List<JobApplicationDto> getApplicationsByStatus(ApplicationStatus status);
    JobApplicationDto updateApplicationStatus(UUID applicationId, ApplicationStatus newStatus);
    List<JobOffer> getPublishedOffers();

    JobOfferDto createJobOffer(JobOfferCreateDto createDto);
    List<JobOfferDto> getAllOffers();
    JobOfferDto getOfferById(UUID offerId);
    JobApplicationDto createApplication(JobApplicationCreateDto createDto);
}

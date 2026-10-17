package com.careertrack.modules.recruitment.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.recruitment.dto.JobApplicationDto;
import com.careertrack.modules.recruitment.entity.*;
import com.careertrack.modules.recruitment.repository.JobApplicationRepository;
import com.careertrack.modules.recruitment.repository.JobOfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecruitmentServiceImplTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private JobOfferRepository jobOfferRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private RecruitmentServiceImpl recruitmentService;

    private UUID applicationId;
    private JobApplication application;
    private Candidate candidate;
    private JobOffer jobOffer;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();

        candidate = Candidate.builder()
                .id(UUID.randomUUID())
                .firstName("Moussa")
                .lastName("Ba")
                .email("moussa.ba@gmail.com")
                .build();

        jobOffer = JobOffer.builder()
                .id(UUID.randomUUID())
                .title("Développeur Java Spring Boot")
                .status(JobOfferStatus.OPEN)
                .build();

        application = JobApplication.builder()
                .id(applicationId)
                .candidate(candidate)
                .jobOffer(jobOffer)
                .status(ApplicationStatus.RECU)
                .build();

        ReflectionTestUtils.setField(recruitmentService, "mailFrom", "no-reply@careertrack.sn");
    }

    @Test
    @DisplayName("Changement de statut Kanban déclenchant l'e-mail de notification au candidat")
    void shouldUpdateStatusAndSendNotificationEmail() {
        when(jobApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArgument(0));

        JobApplicationDto result = recruitmentService.updateApplicationStatus(applicationId, ApplicationStatus.ENTRETIEN);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.ENTRETIEN);
        verify(jobApplicationRepository).save(application);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Récupération de toutes les candidatures")
    void shouldGetAllApplications() {
        when(jobApplicationRepository.findAll()).thenReturn(List.of(application));

        List<JobApplicationDto> results = recruitmentService.getAllApplications();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCandidateFullName()).isEqualTo("Moussa Ba");
    }

    @Test
    @DisplayName("Échec si la candidature n'existe pas")
    void shouldThrowResourceNotFoundWhenApplicationNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(jobApplicationRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recruitmentService.updateApplicationStatus(unknownId, ApplicationStatus.OFFRE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Candidature introuvable");
    }
}

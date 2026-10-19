package com.careertrack.modules.recruitment.service;

import com.careertrack.common.exception.ConflictException;
import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.entity.ContractType;
import com.careertrack.modules.employee.entity.Department;
import com.careertrack.modules.employee.entity.Position;
import com.careertrack.modules.employee.repository.DepartmentRepository;
import com.careertrack.modules.employee.repository.PositionRepository;
import com.careertrack.modules.notification.entity.Notification;
import com.careertrack.modules.notification.repository.NotificationRepository;
import com.careertrack.modules.recruitment.dto.JobApplicationCreateDto;
import com.careertrack.modules.recruitment.dto.JobApplicationDto;
import com.careertrack.modules.recruitment.dto.JobOfferCreateDto;
import com.careertrack.modules.recruitment.dto.JobOfferDto;
import com.careertrack.modules.recruitment.entity.*;
import com.careertrack.modules.recruitment.repository.CandidateRepository;
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
    private CandidateRepository candidateRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private PositionRepository positionRepository;
    @Mock
    private NotificationRepository notificationRepository;
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
    @DisplayName("Changement de statut Kanban déclenchant l'e-mail et la journalisation en base")
    void shouldUpdateStatusAndSendNotificationEmailAndLogNotification() {
        when(jobApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArgument(0));

        JobApplicationDto result = recruitmentService.updateApplicationStatus(applicationId, ApplicationStatus.ENTRETIEN);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.ENTRETIEN);
        verify(jobApplicationRepository).save(application);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        verify(notificationRepository, times(1)).save(any(Notification.class));
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
    @DisplayName("Création d'une offre d'emploi par DRH")
    void shouldCreateJobOffer() {
        Department dept = Department.builder().id(UUID.randomUUID()).name("R&D").build();
        Position pos = Position.builder().id(UUID.randomUUID()).title("Architecte Cloud").build();

        when(departmentRepository.findById(dept.getId())).thenReturn(Optional.of(dept));
        when(positionRepository.findById(pos.getId())).thenReturn(Optional.of(pos));
        when(jobOfferRepository.save(any(JobOffer.class))).thenAnswer(inv -> {
            JobOffer o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        JobOfferCreateDto dto = JobOfferCreateDto.builder()
                .title("Architecte Cloud AWS")
                .departmentId(dept.getId())
                .positionId(pos.getId())
                .contractType(ContractType.CDI)
                .description("Conception d'infrastructures hautement disponibles")
                .requirements("5 ans d'expérience Terraform / Kubernetes")
                .openPositions(2)
                .closingDate(LocalDate.now().plusMonths(2))
                .build();

        JobOfferDto created = recruitmentService.createJobOffer(dto);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Architecte Cloud AWS");
        assertThat(created.getDepartmentName()).isEqualTo("R&D");
        assertThat(created.getPositionTitle()).isEqualTo("Architecte Cloud");
        assertThat(created.getStatus()).isEqualTo(JobOfferStatus.OPEN);
    }

    @Test
    @DisplayName("Dépôt d'une candidature pour une offre")
    void shouldCreateJobApplication() {
        when(jobOfferRepository.findById(jobOffer.getId())).thenReturn(Optional.of(jobOffer));
        when(candidateRepository.findByEmail("candidat@test.sn")).thenReturn(Optional.empty());
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> {
            Candidate c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(jobApplicationRepository.findByJobOfferId(jobOffer.getId())).thenReturn(List.of());
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> {
            JobApplication a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        JobApplicationCreateDto dto = JobApplicationCreateDto.builder()
                .jobOfferId(jobOffer.getId())
                .firstName("Awa")
                .lastName("Gueye")
                .email("candidat@test.sn")
                .phone("+221 77 123 45 67")
                .cvUrl("https://storage.careertrack.sn/cv/awa.pdf")
                .notes("Recommandée par l'équipe technique")
                .build();

        JobApplicationDto created = recruitmentService.createApplication(dto);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCandidateFullName()).isEqualTo("Awa Gueye");
        assertThat(created.getStatus()).isEqualTo(ApplicationStatus.RECU);
    }

    @Test
    @DisplayName("Refus de doublon de candidature pour un même candidat sur la même offre")
    void shouldThrowConflictWhenApplicationAlreadyExists() {
        when(jobOfferRepository.findById(jobOffer.getId())).thenReturn(Optional.of(jobOffer));
        when(candidateRepository.findByEmail(candidate.getEmail())).thenReturn(Optional.of(candidate));
        when(jobApplicationRepository.findByJobOfferId(jobOffer.getId())).thenReturn(List.of(application));

        JobApplicationCreateDto dto = JobApplicationCreateDto.builder()
                .jobOfferId(jobOffer.getId())
                .firstName(candidate.getFirstName())
                .lastName(candidate.getLastName())
                .email(candidate.getEmail())
                .build();

        assertThatThrownBy(() -> recruitmentService.createApplication(dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("existe déjà");
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

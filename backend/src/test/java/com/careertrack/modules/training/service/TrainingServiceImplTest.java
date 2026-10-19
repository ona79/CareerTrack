package com.careertrack.modules.training.service;

import com.careertrack.common.exception.BadRequestException;
import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.common.pdf.PdfGeneratorService;
import com.careertrack.common.pdf.template.TrainingCertificatePdfData;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.training.dto.*;
import com.careertrack.modules.training.entity.*;
import com.careertrack.modules.training.repository.TrainingEnrollmentRepository;
import com.careertrack.modules.training.repository.TrainingRepository;
import com.careertrack.modules.training.repository.TrainingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingServiceImplTest {

    @Mock
    private TrainingRepository trainingRepository;
    @Mock
    private TrainingSessionRepository sessionRepository;
    @Mock
    private TrainingEnrollmentRepository enrollmentRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PdfGeneratorService pdfGeneratorService;

    private TrainingServiceImpl trainingService;

    @BeforeEach
    void setUp() {
        trainingService = new TrainingServiceImpl(
                trainingRepository,
                sessionRepository,
                enrollmentRepository,
                employeeRepository,
                pdfGeneratorService
        );
    }

    @Test
    @DisplayName("Créer un module de formation")
    void shouldCreateTraining() {
        TrainingDto dto = TrainingDto.builder()
                .code("TECH-K8S")
                .title("Kubernetes Avancé")
                .durationHours(30)
                .provider("Dakar Cloud")
                .category("Technique")
                .build();

        Training saved = Training.builder()
                .id(UUID.randomUUID())
                .code(dto.getCode())
                .title(dto.getTitle())
                .durationHours(dto.getDurationHours())
                .provider(dto.getProvider())
                .category(dto.getCategory())
                .build();

        when(trainingRepository.save(any(Training.class))).thenReturn(saved);

        TrainingDto result = trainingService.createTraining(dto);

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getCode()).isEqualTo("TECH-K8S");
    }

    @Test
    @DisplayName("Planifier une session de formation avec dates valides")
    void shouldCreateSession() {
        UUID trainingId = UUID.randomUUID();
        Training training = Training.builder().id(trainingId).title("Kubernetes Avancé").build();

        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.plus(5, ChronoUnit.DAYS);

        TrainingSessionCreateDto dto = TrainingSessionCreateDto.builder()
                .trainingId(trainingId)
                .sessionCode("SESS-K8S-01")
                .startDate(start)
                .endDate(end)
                .location("Salle Dakar Tech")
                .maxParticipants(12)
                .build();

        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training));
        when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(i -> {
            TrainingSession s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        TrainingSessionDto result = trainingService.createSession(dto);

        assertThat(result.getSessionCode()).isEqualTo("SESS-K8S-01");
        assertThat(result.getMaxParticipants()).isEqualTo(12);
        assertThat(result.getStatus()).isEqualTo(TrainingSessionStatus.SCHEDULED);
    }

    @Test
    @DisplayName("Rejeter la planification d'une session si dateFin < dateDebut")
    void shouldRejectSessionWhenDatesAreInvalid() {
        UUID trainingId = UUID.randomUUID();
        Training training = Training.builder().id(trainingId).build();

        Instant start = Instant.now().plus(5, ChronoUnit.DAYS);
        Instant end = Instant.now().plus(2, ChronoUnit.DAYS); // avant start

        TrainingSessionCreateDto dto = TrainingSessionCreateDto.builder()
                .trainingId(trainingId)
                .sessionCode("SESS-INVALID")
                .startDate(start)
                .endDate(end)
                .build();

        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(training));

        assertThatThrownBy(() -> trainingService.createSession(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("antérieure");
    }

    @Test
    @DisplayName("Inscrire un collaborateur à une session disponible")
    void shouldEnrollEmployeeSuccessfully() {
        UUID sessionId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        TrainingSession session = TrainingSession.builder()
                .id(sessionId)
                .maxParticipants(10)
                .status(TrainingSessionStatus.SCHEDULED)
                .build();

        Employee emp = Employee.builder()
                .id(employeeId)
                .firstName("Fatou")
                .lastName("Ndiaye")
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(emp));
        when(enrollmentRepository.existsBySessionIdAndEmployeeId(sessionId, employeeId)).thenReturn(false);
        when(enrollmentRepository.countBySessionIdAndStatusIn(eq(sessionId), any())).thenReturn(5L);

        when(enrollmentRepository.save(any(TrainingEnrollment.class))).thenAnswer(i -> {
            TrainingEnrollment e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        EnrollmentRequestDto dto = EnrollmentRequestDto.builder()
                .sessionId(sessionId)
                .employeeId(employeeId)
                .build();

        TrainingEnrollmentDto result = trainingService.enrollEmployee(dto, UUID.randomUUID());

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(result.getEmployeeFullName()).isEqualTo("Fatou Ndiaye");
    }

    @Test
    @DisplayName("Rejeter l'inscription si la session a atteint sa capacité maximale")
    void shouldRejectEnrollmentWhenSessionIsFull() {
        UUID sessionId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        TrainingSession session = TrainingSession.builder()
                .id(sessionId)
                .maxParticipants(10)
                .status(TrainingSessionStatus.SCHEDULED)
                .build();

        Employee emp = Employee.builder().id(employeeId).build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(emp));
        when(enrollmentRepository.existsBySessionIdAndEmployeeId(sessionId, employeeId)).thenReturn(false);
        when(enrollmentRepository.countBySessionIdAndStatusIn(eq(sessionId), any())).thenReturn(10L); // Complet

        EnrollmentRequestDto dto = EnrollmentRequestDto.builder()
                .sessionId(sessionId)
                .employeeId(employeeId)
                .build();

        assertThatThrownBy(() -> trainingService.enrollEmployee(dto, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Session complète");
    }

    @Test
    @DisplayName("Rejeter l'inscription en cas de doublon")
    void shouldRejectEnrollmentWhenAlreadyEnrolled() {
        UUID sessionId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        TrainingSession session = TrainingSession.builder()
                .id(sessionId)
                .maxParticipants(15)
                .status(TrainingSessionStatus.SCHEDULED)
                .build();

        Employee emp = Employee.builder().id(employeeId).build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(emp));
        when(enrollmentRepository.existsBySessionIdAndEmployeeId(sessionId, employeeId)).thenReturn(true);

        EnrollmentRequestDto dto = EnrollmentRequestDto.builder()
                .sessionId(sessionId)
                .employeeId(employeeId)
                .build();

        assertThatThrownBy(() -> trainingService.enrollEmployee(dto, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("déjà inscrit");
    }

    @Test
    @DisplayName("Mettre à jour le statut à COMPLETED avec note et générer l'URL de certificat")
    void shouldUpdateEnrollmentStatusToCompleted() {
        UUID enrollmentId = UUID.randomUUID();
        TrainingEnrollment enrollment = TrainingEnrollment.builder()
                .id(enrollmentId)
                .status(EnrollmentStatus.CONFIRMED)
                .build();

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(TrainingEnrollment.class))).thenAnswer(i -> i.getArgument(0));

        EnrollmentStatusUpdateDto dto = EnrollmentStatusUpdateDto.builder()
                .status(EnrollmentStatus.COMPLETED)
                .score(new BigDecimal("92.5"))
                .feedback("Compétences validées avec succès")
                .build();

        TrainingEnrollmentDto result = trainingService.updateEnrollmentStatus(enrollmentId, dto);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(result.getScore()).isEqualByComparingTo("92.5");
        assertThat(result.getCertificateUrl()).isEqualTo("/api/v1/trainings/enrollments/" + enrollmentId + "/certificate");
    }

    @Test
    @DisplayName("Générer l'attestation PDF officielle de formation")
    void shouldGenerateCertificatePdf() {
        UUID enrollmentId = UUID.randomUUID();
        Training training = Training.builder()
                .title("Management Bienveillant")
                .provider("ISM Dakar")
                .durationHours(20)
                .code("MGT-LEAD")
                .build();

        TrainingSession session = TrainingSession.builder()
                .training(training)
                .startDate(Instant.now().minus(10, ChronoUnit.DAYS))
                .endDate(Instant.now().minus(2, ChronoUnit.DAYS))
                .build();

        Employee emp = Employee.builder()
                .id(UUID.randomUUID())
                .firstName("Awa")
                .lastName("Sow")
                .employeeNumber("MAT-202")
                .build();

        TrainingEnrollment enrollment = TrainingEnrollment.builder()
                .id(enrollmentId)
                .session(session)
                .employee(emp)
                .status(EnrollmentStatus.COMPLETED)
                .build();

        byte[] expectedPdf = new byte[]{0x25, 0x50, 0x44, 0x46};

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(pdfGeneratorService.generateTrainingCertificatePdf(any(TrainingCertificatePdfData.class))).thenReturn(expectedPdf);

        byte[] result = trainingService.generateCertificatePdf(enrollmentId);

        assertThat(result).isEqualTo(expectedPdf);
        ArgumentCaptor<TrainingCertificatePdfData> captor = ArgumentCaptor.forClass(TrainingCertificatePdfData.class);
        verify(pdfGeneratorService).generateTrainingCertificatePdf(captor.capture());
        assertThat(captor.getValue().employeeName()).isEqualTo("Awa Sow");
        assertThat(captor.getValue().trainingTitle()).isEqualTo("Management Bienveillant");
        assertThat(captor.getValue().durationHours()).isEqualTo(20);
    }
}

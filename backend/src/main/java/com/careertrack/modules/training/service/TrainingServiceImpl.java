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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TrainingServiceImpl implements TrainingService {

    private final TrainingRepository trainingRepository;
    private final TrainingSessionRepository sessionRepository;
    private final TrainingEnrollmentRepository enrollmentRepository;
    private final EmployeeRepository employeeRepository;
    private final PdfGeneratorService pdfGeneratorService;

    // ─── CATALOGUE FORMATIONS ──────────────────────────────────────────────────

    @Override
    public List<TrainingDto> getAllTrainings() {
        return trainingRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public TrainingDto getTrainingById(UUID id) {
        Training t = trainingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable : " + id));
        return mapToDto(t);
    }

    @Override
    @Transactional
    public TrainingDto createTraining(TrainingDto dto) {
        Training t = Training.builder()
                .code(dto.getCode())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .durationHours(dto.getDurationHours())
                .provider(dto.getProvider())
                .category(dto.getCategory())
                .build();
        return mapToDto(trainingRepository.save(t));
    }

    // ─── SESSIONS DE FORMATION ─────────────────────────────────────────────────

    @Override
    public List<TrainingSessionDto> getAllSessions() {
        return sessionRepository.findAll().stream()
                .map(this::mapSessionToDto)
                .toList();
    }

    @Override
    public List<TrainingSessionDto> getSessionsByTraining(UUID trainingId) {
        return sessionRepository.findByTrainingId(trainingId).stream()
                .map(this::mapSessionToDto)
                .toList();
    }

    @Override
    public TrainingSessionDto getSessionById(UUID sessionId) {
        TrainingSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session de formation introuvable : " + sessionId));
        return mapSessionToDto(s);
    }

    @Override
    @Transactional
    public TrainingSessionDto createSession(TrainingSessionCreateDto dto) {
        Training training = trainingRepository.findById(dto.getTrainingId())
                .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable : " + dto.getTrainingId()));

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BadRequestException("La date de fin ne peut pas être antérieure à la date de début");
        }

        TrainingSession session = TrainingSession.builder()
                .training(training)
                .sessionCode(dto.getSessionCode())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .location(dto.getLocation())
                .maxParticipants(dto.getMaxParticipants() > 0 ? dto.getMaxParticipants() : 15)
                .status(TrainingSessionStatus.SCHEDULED)
                .build();

        return mapSessionToDto(sessionRepository.save(session));
    }

    @Override
    @Transactional
    public TrainingSessionDto updateSessionStatus(UUID sessionId, TrainingSessionStatus status) {
        TrainingSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session de formation introuvable : " + sessionId));
        s.setStatus(status);
        return mapSessionToDto(sessionRepository.save(s));
    }

    // ─── INSCRIPTIONS (ENROLLMENTS) ───────────────────────────────────────────

    @Override
    @Transactional
    public TrainingEnrollmentDto enrollEmployee(EnrollmentRequestDto dto, UUID currentUserId) {
        TrainingSession session = sessionRepository.findById(dto.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session de formation introuvable : " + dto.getSessionId()));

        if (session.getStatus() == TrainingSessionStatus.CANCELLED) {
            throw new BadRequestException("Impossible de s'inscrire : cette session est annulée");
        }
        if (session.getStatus() == TrainingSessionStatus.COMPLETED) {
            throw new BadRequestException("Impossible de s'inscrire : cette session est déjà terminée");
        }

        final UUID finalEmpId = (dto.getEmployeeId() != null)
                ? dto.getEmployeeId()
                : employeeRepository.findByUserId(currentUserId)
                        .orElseThrow(() -> new ResourceNotFoundException("Profil collaborateur introuvable pour l'utilisateur connecté"))
                        .getId();

        Employee employee = employeeRepository.findById(finalEmpId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborateur introuvable : " + finalEmpId));

        // Vérification doublon
        if (enrollmentRepository.existsBySessionIdAndEmployeeId(session.getId(), employee.getId())) {
            throw new BadRequestException("Le collaborateur est déjà inscrit à cette session de formation");
        }

        // Vérification capacité max
        long activeCount = enrollmentRepository.countBySessionIdAndStatusIn(
                session.getId(),
                List.of(EnrollmentStatus.REQUESTED, EnrollmentStatus.CONFIRMED, EnrollmentStatus.ATTENDED, EnrollmentStatus.COMPLETED)
        );
        if (activeCount >= session.getMaxParticipants()) {
            throw new BadRequestException("Session complète : la capacité maximale de " + session.getMaxParticipants() + " places est atteinte");
        }

        TrainingEnrollment enrollment = TrainingEnrollment.builder()
                .session(session)
                .employee(employee)
                .enrollmentDate(LocalDate.now())
                .status(EnrollmentStatus.CONFIRMED)
                .build();

        return mapEnrollmentToDto(enrollmentRepository.save(enrollment));
    }

    @Override
    public List<TrainingEnrollmentDto> getEnrollmentsByEmployee(UUID employeeId) {
        return enrollmentRepository.findByEmployeeId(employeeId).stream()
                .map(this::mapEnrollmentToDto)
                .toList();
    }

    @Override
    public List<TrainingEnrollmentDto> getMyEnrollments(UUID currentUserId) {
        Optional<Employee> empOpt = employeeRepository.findByUserId(currentUserId);
        if (empOpt.isEmpty()) {
            return Collections.emptyList();
        }
        return enrollmentRepository.findByEmployeeId(empOpt.get().getId()).stream()
                .map(this::mapEnrollmentToDto)
                .toList();
    }

    @Override
    public List<TrainingEnrollmentDto> getEnrollmentsBySession(UUID sessionId) {
        return enrollmentRepository.findBySessionId(sessionId).stream()
                .map(this::mapEnrollmentToDto)
                .toList();
    }

    @Override
    @Transactional
    public TrainingEnrollmentDto updateEnrollmentStatus(UUID enrollmentId, EnrollmentStatusUpdateDto dto) {
        TrainingEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable : " + enrollmentId));

        enrollment.setStatus(dto.getStatus());
        if (dto.getScore() != null) {
            enrollment.setScore(dto.getScore());
        }
        if (dto.getFeedback() != null && !dto.getFeedback().isBlank()) {
            enrollment.setFeedback(dto.getFeedback());
        }

        if (dto.getStatus() == EnrollmentStatus.COMPLETED) {
            enrollment.setCertificateUrl("/api/v1/trainings/enrollments/" + enrollment.getId() + "/certificate");
        }

        return mapEnrollmentToDto(enrollmentRepository.save(enrollment));
    }

    // ─── ATTESTATION DE FORMATION PDF ─────────────────────────────────────────

    @Override
    public byte[] generateCertificatePdf(UUID enrollmentId) {
        TrainingEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable : " + enrollmentId));

        TrainingSession session = enrollment.getSession();
        Training training = session != null ? session.getTraining() : null;
        Employee employee = enrollment.getEmployee();
        if (employee != null) {
            employee = employeeRepository.findById(employee.getId()).orElse(employee);
        }

        LocalDate completionDate = session != null && session.getEndDate() != null
                ? session.getEndDate().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();

        LocalDate startDate = session != null && session.getStartDate() != null
                ? session.getStartDate().atZone(ZoneId.systemDefault()).toLocalDate()
                : completionDate.minusDays(5);

        String certNumber = "CERT-" + enrollment.getId().toString().substring(0, 8).toUpperCase();

        TrainingCertificatePdfData pdfData = new TrainingCertificatePdfData(
                certNumber,
                employee != null ? employee.getFullName() : "N/A",
                employee != null ? employee.getEmployeeNumber() : "N/A",
                employee != null && employee.getPosition() != null ? employee.getPosition().getTitle() : "Collaborateur",
                training != null ? training.getTitle() : "Formation Continue",
                training != null && training.getProvider() != null ? training.getProvider() : "CareerTrack Academy",
                startDate,
                completionDate,
                training != null && training.getDurationHours() > 0 ? training.getDurationHours() : 35,
                "Acquisition et validation opérationnelle des compétences : " + (training != null ? training.getTitle() : "")
        );

        return pdfGeneratorService.generateTrainingCertificatePdf(pdfData);
    }

    // ─── MAPPERS ──────────────────────────────────────────────────────────────

    private TrainingDto mapToDto(Training t) {
        return TrainingDto.builder()
                .id(t.getId())
                .code(t.getCode())
                .title(t.getTitle())
                .description(t.getDescription())
                .durationHours(t.getDurationHours())
                .provider(t.getProvider())
                .category(t.getCategory())
                .build();
    }

    private TrainingSessionDto mapSessionToDto(TrainingSession s) {
        long count = enrollmentRepository.countBySessionIdAndStatusIn(
                s.getId(),
                List.of(EnrollmentStatus.REQUESTED, EnrollmentStatus.CONFIRMED, EnrollmentStatus.ATTENDED, EnrollmentStatus.COMPLETED)
        );

        return TrainingSessionDto.builder()
                .id(s.getId())
                .trainingId(s.getTraining() != null ? s.getTraining().getId() : null)
                .trainingTitle(s.getTraining() != null ? s.getTraining().getTitle() : null)
                .trainingCode(s.getTraining() != null ? s.getTraining().getCode() : null)
                .sessionCode(s.getSessionCode())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .location(s.getLocation())
                .maxParticipants(s.getMaxParticipants())
                .enrolledParticipantsCount((int) count)
                .status(s.getStatus())
                .build();
    }

    private TrainingEnrollmentDto mapEnrollmentToDto(TrainingEnrollment e) {
        TrainingSession s = e.getSession();
        Training t = s != null ? s.getTraining() : null;
        Employee emp = e.getEmployee();

        return TrainingEnrollmentDto.builder()
                .id(e.getId())
                .sessionId(s != null ? s.getId() : null)
                .sessionCode(s != null ? s.getSessionCode() : null)
                .trainingId(t != null ? t.getId() : null)
                .trainingTitle(t != null ? t.getTitle() : null)
                .durationHours(t != null ? t.getDurationHours() : null)
                .provider(t != null ? t.getProvider() : null)
                .sessionStartDate(s != null ? s.getStartDate() : null)
                .sessionEndDate(s != null ? s.getEndDate() : null)
                .employeeId(emp != null ? emp.getId() : null)
                .employeeFullName(emp != null ? emp.getFullName() : null)
                .employeeNumber(emp != null ? emp.getEmployeeNumber() : null)
                .enrollmentDate(e.getEnrollmentDate())
                .status(e.getStatus())
                .score(e.getScore())
                .certificateUrl(e.getCertificateUrl())
                .feedback(e.getFeedback())
                .build();
    }
}

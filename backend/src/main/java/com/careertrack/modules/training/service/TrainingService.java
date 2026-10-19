package com.careertrack.modules.training.service;

import com.careertrack.modules.training.dto.*;
import com.careertrack.modules.training.entity.TrainingSessionStatus;

import java.util.List;
import java.util.UUID;

public interface TrainingService {
    // Catalogue Formations
    List<TrainingDto> getAllTrainings();
    TrainingDto getTrainingById(UUID id);
    TrainingDto createTraining(TrainingDto dto);

    // Sessions de Formation
    List<TrainingSessionDto> getAllSessions();
    List<TrainingSessionDto> getSessionsByTraining(UUID trainingId);
    TrainingSessionDto getSessionById(UUID sessionId);
    TrainingSessionDto createSession(TrainingSessionCreateDto dto);
    TrainingSessionDto updateSessionStatus(UUID sessionId, TrainingSessionStatus status);

    // Inscriptions (Enrollments)
    TrainingEnrollmentDto enrollEmployee(EnrollmentRequestDto dto, UUID currentUserId);
    List<TrainingEnrollmentDto> getEnrollmentsByEmployee(UUID employeeId);
    List<TrainingEnrollmentDto> getMyEnrollments(UUID currentUserId);
    List<TrainingEnrollmentDto> getEnrollmentsBySession(UUID sessionId);
    TrainingEnrollmentDto updateEnrollmentStatus(UUID enrollmentId, EnrollmentStatusUpdateDto dto);

    // Attestation PDF
    byte[] generateCertificatePdf(UUID enrollmentId);
}

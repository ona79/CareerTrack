package com.careertrack.modules.training.repository;

import com.careertrack.modules.training.entity.EnrollmentStatus;
import com.careertrack.modules.training.entity.TrainingEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainingEnrollmentRepository extends JpaRepository<TrainingEnrollment, UUID> {
    List<TrainingEnrollment> findByEmployeeId(UUID employeeId);
    List<TrainingEnrollment> findBySessionId(UUID sessionId);
    Optional<TrainingEnrollment> findBySessionIdAndEmployeeId(UUID sessionId, UUID employeeId);
    boolean existsBySessionIdAndEmployeeId(UUID sessionId, UUID employeeId);
    long countBySessionIdAndStatusIn(UUID sessionId, Collection<EnrollmentStatus> statuses);
}

package com.careertrack.modules.training.repository;

import com.careertrack.modules.training.entity.TrainingEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrainingEnrollmentRepository extends JpaRepository<TrainingEnrollment, UUID> {
    List<TrainingEnrollment> findByEmployeeId(UUID employeeId);
    List<TrainingEnrollment> findBySessionId(UUID sessionId);
}

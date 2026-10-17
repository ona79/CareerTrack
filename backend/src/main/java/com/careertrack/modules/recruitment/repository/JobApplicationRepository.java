package com.careertrack.modules.recruitment.repository;

import com.careertrack.modules.recruitment.entity.ApplicationStatus;
import com.careertrack.modules.recruitment.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    List<JobApplication> findByJobOfferId(UUID jobOfferId);
    List<JobApplication> findByStatus(ApplicationStatus status);
}

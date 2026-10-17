package com.careertrack.modules.recruitment.repository;

import com.careertrack.modules.recruitment.entity.JobOffer;
import com.careertrack.modules.recruitment.entity.JobOfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, UUID> {
    List<JobOffer> findByStatus(JobOfferStatus status);
}

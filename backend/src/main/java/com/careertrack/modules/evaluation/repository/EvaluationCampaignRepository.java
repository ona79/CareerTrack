package com.careertrack.modules.evaluation.repository;

import com.careertrack.modules.evaluation.entity.CampaignStatus;
import com.careertrack.modules.evaluation.entity.EvaluationCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationCampaignRepository extends JpaRepository<EvaluationCampaign, UUID> {
    List<EvaluationCampaign> findByCampaignYear(int year);
    List<EvaluationCampaign> findByStatus(CampaignStatus status);
}

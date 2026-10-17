package com.careertrack.modules.evaluation.repository;

import com.careertrack.modules.evaluation.entity.Evaluation;
import com.careertrack.modules.evaluation.entity.EvaluationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {
    List<Evaluation> findByCampaignId(UUID campaignId);
    List<Evaluation> findByEmployeeId(UUID employeeId);
    List<Evaluation> findByEvaluatorId(UUID evaluatorId);
    Optional<Evaluation> findByCampaignIdAndEmployeeId(UUID campaignId, UUID employeeId);
    List<Evaluation> findByStatus(EvaluationStatus status);
}

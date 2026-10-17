package com.careertrack.modules.promotion.repository;

import com.careertrack.modules.promotion.entity.PromotionProposal;
import com.careertrack.modules.promotion.entity.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionProposalRepository extends JpaRepository<PromotionProposal, UUID> {
    List<PromotionProposal> findByEmployeeId(UUID employeeId);
    List<PromotionProposal> findByStatus(PromotionStatus status);
}

package com.careertrack.modules.promotion.service;

import com.careertrack.modules.promotion.dto.PromotionProposalDto;
import com.careertrack.modules.promotion.entity.PromotionStatus;

import java.util.List;
import java.util.UUID;

public interface PromotionService {
    List<PromotionProposalDto> getAllProposals();
    List<PromotionProposalDto> getProposalsByEmployee(UUID employeeId);
    PromotionProposalDto createProposal(PromotionProposalDto dto);
    PromotionProposalDto updateStatus(UUID proposalId, PromotionStatus status, String comment);

    com.careertrack.modules.promotion.dto.SalarySimulationResultDto simulateSalaryImpact(com.careertrack.modules.promotion.dto.SalarySimulationRequestDto requestDto);
    List<com.careertrack.modules.promotion.dto.SalarySimulationResultDto> getSalarySimulations();
    com.careertrack.modules.promotion.dto.SalarySimulationResultDto getSalarySimulationById(UUID simulationId);
}

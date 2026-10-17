package com.careertrack.modules.promotion.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.Position;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.employee.repository.PositionRepository;
import com.careertrack.modules.promotion.dto.PromotionProposalDto;
import com.careertrack.modules.promotion.entity.PromotionProposal;
import com.careertrack.modules.promotion.entity.PromotionStatus;
import com.careertrack.modules.promotion.repository.PromotionProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PromotionServiceImpl implements PromotionService {

    private final PromotionProposalRepository proposalRepository;
    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;

    public PromotionServiceImpl(
            PromotionProposalRepository proposalRepository,
            EmployeeRepository employeeRepository,
            PositionRepository positionRepository) {
        this.proposalRepository = proposalRepository;
        this.employeeRepository = employeeRepository;
        this.positionRepository = positionRepository;
    }

    @Override
    public List<PromotionProposalDto> getAllProposals() {
        return proposalRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public List<PromotionProposalDto> getProposalsByEmployee(UUID employeeId) {
        return proposalRepository.findByEmployeeId(employeeId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public PromotionProposalDto createProposal(PromotionProposalDto dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + dto.getEmployeeId()));
        Position position = positionRepository.findById(dto.getProposedPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("Poste introuvable : " + dto.getProposedPositionId()));

        PromotionProposal proposal = PromotionProposal.builder()
                .employee(employee)
                .targetPosition(position)
                .currentSalary(dto.getCurrentSalary())
                .proposedSalary(dto.getProposedSalary())
                .justification(dto.getJustification())
                .status(PromotionStatus.SUBMITTED)
                .build();

        return mapToDto(proposalRepository.save(proposal));
    }

    @Override
    @Transactional
    public PromotionProposalDto updateStatus(UUID proposalId, PromotionStatus status, String comment) {
        PromotionProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposition introuvable : " + proposalId));
        proposal.setStatus(status);
        proposal.setValidatedAt(Instant.now());
        return mapToDto(proposalRepository.save(proposal));
    }

    private PromotionProposalDto mapToDto(PromotionProposal p) {
        BigDecimal increase = BigDecimal.ZERO;
        if (p.getCurrentSalary() != null && p.getProposedSalary() != null && p.getCurrentSalary().compareTo(BigDecimal.ZERO) > 0) {
            increase = p.getProposedSalary().subtract(p.getCurrentSalary())
                    .divide(p.getCurrentSalary(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return PromotionProposalDto.builder()
                .id(p.getId())
                .employeeId(p.getEmployee() != null ? p.getEmployee().getId() : null)
                .employeeFullName(p.getEmployee() != null ? p.getEmployee().getFullName() : null)
                .proposedPositionId(p.getTargetPosition() != null ? p.getTargetPosition().getId() : null)
                .proposedPositionTitle(p.getTargetPosition() != null ? p.getTargetPosition().getTitle() : null)
                .currentSalary(p.getCurrentSalary())
                .proposedSalary(p.getProposedSalary())
                .percentageIncrease(increase)
                .justification(p.getJustification())
                .status(p.getStatus())
                .build();
    }
}

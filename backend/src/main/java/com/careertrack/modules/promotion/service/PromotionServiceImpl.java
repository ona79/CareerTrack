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
import com.careertrack.common.security.EmployeeSecurityService;
import com.careertrack.modules.employee.entity.Contract;
import com.careertrack.modules.employee.entity.PositionHistory;
import com.careertrack.modules.employee.entity.SalaryHistory;
import com.careertrack.modules.employee.repository.ContractRepository;
import com.careertrack.modules.employee.repository.PositionHistoryRepository;
import com.careertrack.modules.employee.repository.SalaryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionProposalRepository proposalRepository;
    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;
    private final ContractRepository contractRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;
    private final PositionHistoryRepository positionHistoryRepository;
    private final EmployeeSecurityService employeeSecurity;

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

        if (status == PromotionStatus.APPROVED) {
            Employee employee = proposal.getEmployee();
            if (employee != null) {
                Employee currentDrh = employeeSecurity.getCurrentEmployee().orElse(null);
                Position previousPosition = employee.getPosition();
                Position targetPosition = proposal.getTargetPosition();

                // 1. Historique de poste si changement de poste
                if (targetPosition != null && (previousPosition == null || !previousPosition.getId().equals(targetPosition.getId()))) {
                    PositionHistory ph = PositionHistory.builder()
                            .employee(employee)
                            .previousPosition(previousPosition)
                            .newPosition(targetPosition)
                            .previousDepartment(employee.getDepartment())
                            .newDepartment(employee.getDepartment())
                            .effectiveDate(LocalDate.now())
                            .changeReason("Promotion validée : " + (proposal.getJustification() != null ? proposal.getJustification() : "Avancement professionnel"))
                            .changedBy(currentDrh)
                            .build();
                    positionHistoryRepository.save(ph);
                    employee.setPosition(targetPosition);
                }

                // 2. Historique de salaire et mise à jour du contrat si revalorisation salariale
                if (proposal.getProposedSalary() != null) {
                    BigDecimal previousSalary = proposal.getCurrentSalary() != null ? proposal.getCurrentSalary() : BigDecimal.ZERO;
                    Optional<Contract> activeContractOpt = contractRepository.findByEmployeeIdAndActiveTrue(employee.getId());
                    if (activeContractOpt.isPresent()) {
                        Contract contract = activeContractOpt.get();
                        previousSalary = contract.getGrossSalary() != null ? contract.getGrossSalary() : previousSalary;
                        contract.setGrossSalary(proposal.getProposedSalary());
                        contractRepository.save(contract);
                    }

                    SalaryHistory sh = SalaryHistory.builder()
                            .employee(employee)
                            .previousSalary(previousSalary)
                            .newSalary(proposal.getProposedSalary())
                            .effectiveDate(LocalDate.now())
                            .changeReason("Promotion validée" + (comment != null && !comment.isBlank() ? " : " + comment : ""))
                            .approvedBy(currentDrh)
                            .build();
                    salaryHistoryRepository.save(sh);
                }

                employeeRepository.save(employee);
            }
        }

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

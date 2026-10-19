package com.careertrack.modules.employee.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.dto.SalaryHistoryDto;
import com.careertrack.modules.employee.entity.SalaryHistory;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.employee.repository.SalaryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeHistoryService {

    private final EmployeeRepository employeeRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;

    @Transactional(readOnly = true)
    public List<SalaryHistoryDto> getSalaryHistory(UUID employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employé", "id", employeeId);
        }
        return salaryHistoryRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId).stream()
                // On ajoute le type (SalaryHistory h) pour aider Java
                .map((SalaryHistory h) -> SalaryHistoryDto.builder()
                        .id(h.getId())
                        .employeeId(h.getEmployee().getId())
                        .previousSalary(h.getPreviousSalary())
                        .newSalary(h.getNewSalary())
                        .changeReason(h.getChangeReason())
                        .effectiveDate(h.getEffectiveDate())
                        .approvedById(h.getApprovedBy() != null ? h.getApprovedBy().getId() : null)
                        .approvedByName(h.getApprovedBy() != null ? h.getApprovedBy().getFullName() : null)
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();
    }
}
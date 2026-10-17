package com.careertrack.modules.orgchart.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.orgchart.dto.OrgNodeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrgChartServiceImpl implements OrgChartService {

    private final EmployeeRepository employeeRepository;

    public OrgChartServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrgNodeDto> getCompanyOrgTree() {
        // Recherche des dirigeants racine (manager IS NULL, par ex: Directeur Général)
        List<Employee> rootEmployees = employeeRepository.findByManagerIsNull();

        return rootEmployees.stream()
                .map(this::buildNodeRecursively)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrgNodeDto getSubtreeForEmployee(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", "id", employeeId));

        return buildNodeRecursively(employee);
    }

    private OrgNodeDto buildNodeRecursively(Employee employee) {
        List<Employee> subordinates = employeeRepository.findByManagerId(employee.getId());

        List<OrgNodeDto> subordinateNodes = subordinates.stream()
                .map(this::buildNodeRecursively)
                .collect(Collectors.toList());

        return OrgNodeDto.builder()
                .id(employee.getId())
                .name(employee.getFullName())
                .employeeNumber(employee.getEmployeeNumber())
                .title(employee.getPosition() != null ? employee.getPosition().getTitle() : "Poste non défini")
                .department(employee.getDepartment() != null ? employee.getDepartment().getName() : "Département non défini")
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .subordinates(subordinateNodes)
                .build();
    }
}

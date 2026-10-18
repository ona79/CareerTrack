package com.careertrack.modules.employee.service;

import com.careertrack.common.exception.BadRequestException;
import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.common.payload.PageResponse;
import com.careertrack.modules.employee.dto.EmployeeCreateDto;
import com.careertrack.modules.employee.dto.EmployeeResponseDto;
import com.careertrack.modules.employee.dto.EmployeeSummaryDto;
import com.careertrack.modules.employee.entity.Department;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.Position;
import com.careertrack.modules.employee.mapper.EmployeeMapper;
import com.careertrack.modules.employee.repository.DepartmentRepository;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.employee.repository.EmployeeSpecifications;
import com.careertrack.modules.employee.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.careertrack.modules.employee.entity.EmployeeStatus;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional
    public EmployeeResponseDto createEmployee(EmployeeCreateDto createDto) {
        if (employeeRepository.findByEmployeeNumber(createDto.getEmployeeNumber()).isPresent()) {
            throw new BadRequestException("Un employé avec le matricule " + createDto.getEmployeeNumber() + " existe déjà");
        }
        if (employeeRepository.findByEmail(createDto.getEmail()).isPresent()) {
            throw new BadRequestException("Un employé avec l'email " + createDto.getEmail() + " existe déjà");
        }

        Employee employee = employeeMapper.toEntity(createDto);

        if (createDto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(createDto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Département", "id", createDto.getDepartmentId()));
            employee.setDepartment(department);
        }

        if (createDto.getPositionId() != null) {
            Position position = positionRepository.findById(createDto.getPositionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Poste", "id", createDto.getPositionId()));
            employee.setPosition(position);
        }

        if (createDto.getManagerId() != null) {
            Employee manager = employeeRepository.findById(createDto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", createDto.getManagerId()));
            employee.setManager(manager);
        }

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDto getEmployeeById(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", "id", id));
        return employeeMapper.toDto(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDto getEmployeeByNumber(String employeeNumber) {
        Employee employee = employeeRepository.findByEmployeeNumber(employeeNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", "matricule", employeeNumber));
        return employeeMapper.toDto(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeSummaryDto> getAllEmployees(Pageable pageable, String search, UUID departmentId, UUID teamOfManagerId) {
        Specification<Employee> spec = EmployeeSpecifications.withDetails()
                .and(EmployeeSpecifications.inDepartment(departmentId))
                .and(EmployeeSpecifications.matchesSearch(search))
                .and(EmployeeSpecifications.inTeamOf(teamOfManagerId));
        Page<Employee> page = employeeRepository.findAll(spec, pageable);
        List<EmployeeSummaryDto> content = page.getContent().stream()
                .map(this::toSummary)
                .toList();

        return PageResponse.<EmployeeSummaryDto>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private EmployeeSummaryDto toSummary(Employee e) {
        return EmployeeSummaryDto.builder()
                .id(e.getId())
                .employeeNumber(e.getEmployeeNumber())
                .fullName(e.getFullName())
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .positionTitle(e.getPosition() != null ? e.getPosition().getTitle() : null)
                .status(e.getStatus())
                .hireDate(e.getHireDate())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> getEmployeesByDepartment(UUID departmentId) {
        return employeeMapper.toDtoList(employeeRepository.findByDepartmentId(departmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> getSubordinates(UUID managerId) {
        return employeeMapper.toDtoList(employeeRepository.findByManagerId(managerId));
    }

    @Override
    @Transactional
    public EmployeeResponseDto updateEmployee(UUID id, EmployeeCreateDto updateDto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", "id", id));

        employee.setFirstName(updateDto.getFirstName());
        employee.setLastName(updateDto.getLastName());
        employee.setNationalId(updateDto.getNationalId());
        employee.setEmail(updateDto.getEmail());
        employee.setPhone(updateDto.getPhone());
        employee.setAddress(updateDto.getAddress());
        employee.setBirthDate(updateDto.getBirthDate());
        employee.setHireDate(updateDto.getHireDate());
        employee.setStatus(updateDto.getStatus());

        if (updateDto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(updateDto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Département", "id", updateDto.getDepartmentId()));
            employee.setDepartment(department);
        }

        if (updateDto.getPositionId() != null) {
            Position position = positionRepository.findById(updateDto.getPositionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Poste", "id", updateDto.getPositionId()));
            employee.setPosition(position);
        }

        if (updateDto.getManagerId() != null) {
            Employee manager = employeeRepository.findById(updateDto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", updateDto.getManagerId()));
            employee.setManager(manager);
        }

        Employee updated = employeeRepository.save(employee);
        return employeeMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteEmployee(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", "id", id));
        employee.setStatus(EmployeeStatus.TERMINATED);
        employeeRepository.save(employee);
    }
}
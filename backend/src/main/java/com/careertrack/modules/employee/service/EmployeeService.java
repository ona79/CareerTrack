package com.careertrack.modules.employee.service;

import com.careertrack.common.payload.PageResponse;
import com.careertrack.modules.employee.dto.EmployeeCreateDto;
import com.careertrack.modules.employee.dto.EmployeeResponseDto;
import com.careertrack.modules.employee.dto.EmployeeSummaryDto;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface EmployeeService {
    EmployeeResponseDto createEmployee(EmployeeCreateDto createDto);
    EmployeeResponseDto getEmployeeById(UUID id);
    EmployeeResponseDto getEmployeeByNumber(String employeeNumber);
    PageResponse<EmployeeSummaryDto> getAllEmployees(Pageable pageable, String search, UUID departmentId);
    List<EmployeeResponseDto> getEmployeesByDepartment(UUID departmentId);
    List<EmployeeResponseDto> getSubordinates(UUID managerId);
    EmployeeResponseDto updateEmployee(UUID id, EmployeeCreateDto updateDto);
    void deleteEmployee(UUID id);
}
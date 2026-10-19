package com.careertrack.modules.employee.service;

import com.careertrack.common.payload.PageResponse;
import com.careertrack.modules.employee.dto.*;
import com.careertrack.modules.employee.entity.EmployeeStatus;

import java.util.List;
import java.util.UUID;

public interface EmployeeService {

    EmployeeResponseDto createEmployee(EmployeeCreateDto createDto);

    EmployeeResponseDto getEmployeeById(UUID id);

    EmployeeDetailDto getEmployeeDetail(UUID id);

    EmployeeResponseDto getEmployeeByNumber(String employeeNumber);

    PageResponse<EmployeeSummaryDto> getAllEmployees(
            int page,
            int size,
            String search,
            UUID departmentId,
            EmployeeStatus status,
            String sortBy,
            String sortDir
    );

    List<EmployeeResponseDto> getEmployeesByDepartment(UUID departmentId);

    List<EmployeeResponseDto> getSubordinates(UUID managerId);

    EmployeeDetailDto updateEmployee(UUID id, EmployeeUpdateDto updateDto);

    EmployeeDetailDto updateSelfContact(UUID id, EmployeeSelfUpdateDto contactDto);

    void deactivateEmployee(UUID id, EmployeeDeactivateDto deactivateDto);

    EmployeeHistoryDto getEmployeeHistory(UUID id);

    void deleteEmployee(UUID id);
}

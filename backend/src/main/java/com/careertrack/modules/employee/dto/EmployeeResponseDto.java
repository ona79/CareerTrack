package com.careertrack.modules.employee.dto;

import com.careertrack.modules.employee.entity.EmployeeStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponseDto {
    private UUID id;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private String nationalId;
    private String email;
    private String phone;
    private String address;
    private LocalDate birthDate;
    private LocalDate hireDate;

    private UUID departmentId;
    private String departmentName;

    private UUID positionId;
    private String positionTitle;

    private UUID managerId;
    private String managerName;

    private EmployeeStatus status;
}

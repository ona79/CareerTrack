package com.careertrack.modules.employee.dto;

import com.careertrack.modules.document.dto.DocumentResponseDto;
import com.careertrack.modules.employee.entity.ContractType;
import com.careertrack.modules.employee.entity.EmployeeStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDetailDto {

    private IdentityBlock identity;
    private PersonalDataBlock personalData;
    private ContractBlock contract;
    private List<SalaryHistoryDto> salaryHistory;
    private List<PositionHistoryDto> positionHistory;
    private List<DocumentResponseDto> documents;
    private PermissionsBlock permissions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IdentityBlock {
        private UUID id;
        private String employeeNumber;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private LocalDate hireDate;
        private UUID departmentId;
        private String departmentName;
        private UUID positionId;
        private String positionTitle;
        private UUID managerId;
        private String managerName;
        private EmployeeStatus status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PersonalDataBlock {
        private String nationalId;
        private LocalDate birthDate;
        private String phone;
        private String address;
        private String maritalStatus;
        private Integer dependentsCount;
        private Boolean isCadre;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContractBlock {
        private UUID id;
        private ContractType contractType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean active;
        private BigDecimal grossSalary;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PermissionsBlock {
        private boolean canEdit;
        private boolean canDeactivate;
        private boolean canEditContact;
    }
}

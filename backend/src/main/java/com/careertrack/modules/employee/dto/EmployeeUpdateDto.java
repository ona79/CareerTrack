package com.careertrack.modules.employee.dto;

import com.careertrack.modules.employee.entity.EmployeeStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeUpdateDto {

    private String employeeNumber;

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @NotBlank(message = "Le numéro CNI est obligatoire")
    private String nationalId;

    @Email(message = "Adresse e-mail invalide")
    @NotBlank(message = "L'adresse e-mail est obligatoire")
    private String email;

    private String phone;
    private String address;

    @NotNull(message = "La date de naissance est obligatoire")
    private LocalDate birthDate;

    @NotNull(message = "La date d'embauche est obligatoire")
    private LocalDate hireDate;

    private UUID departmentId;
    private UUID positionId;
    private UUID managerId;

    private EmployeeStatus status;

    private Boolean isCadre;
    private String maritalStatus;
    private Integer dependentsCount;

    private BigDecimal grossSalary;
    private String changeReason;
}

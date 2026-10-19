package com.careertrack.modules.employee.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSelfUpdateDto {

    @Size(max = 50, message = "Le téléphone ne doit pas dépasser 50 caractères")
    private String phone;

    @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
    private String address;
}

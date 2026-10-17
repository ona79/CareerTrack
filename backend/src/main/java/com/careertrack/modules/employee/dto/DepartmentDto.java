package com.careertrack.modules.employee.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentDto {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private UUID managerId;
    private String managerName;
}

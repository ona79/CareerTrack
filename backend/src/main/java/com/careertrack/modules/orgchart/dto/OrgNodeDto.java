package com.careertrack.modules.orgchart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Nœud de l'arbre hiérarchique de l'entreprise")
public class OrgNodeDto {

    private UUID id;
    private String name;
    private String employeeNumber;
    private String title;
    private String department;
    private String email;
    private String phone;
    private Integer subordinateCount;

    @Builder.Default
    private List<OrgNodeDto> subordinates = new ArrayList<>();
}

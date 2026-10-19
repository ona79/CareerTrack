package com.careertrack.modules.recruitment.dto;

import com.careertrack.modules.employee.entity.ContractType;
import com.careertrack.modules.recruitment.entity.JobOfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobOfferDto {
    private UUID id;
    private String code;
    private String title;
    private UUID departmentId;
    private String departmentName;
    private UUID positionId;
    private String positionTitle;
    private ContractType contractType;
    private String description;
    private String requirements;
    private JobOfferStatus status;
    private int openPositions;
    private LocalDate closingDate;
    private int applicationsCount;
}

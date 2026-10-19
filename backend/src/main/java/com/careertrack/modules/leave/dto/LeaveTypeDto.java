package com.careertrack.modules.leave.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTypeDto {
    private UUID id;
    private String code;
    private String name;
    private Integer maxDaysPerYear;
    private boolean isPaid;
    private boolean requiresJustification;
}

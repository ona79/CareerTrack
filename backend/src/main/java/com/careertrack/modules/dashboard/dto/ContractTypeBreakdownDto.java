package com.careertrack.modules.dashboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractTypeBreakdownDto {
    private String contractType;
    private long count;
    private double percentage;
}

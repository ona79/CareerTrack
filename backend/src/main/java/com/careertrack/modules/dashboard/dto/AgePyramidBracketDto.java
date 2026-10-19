package com.careertrack.modules.dashboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgePyramidBracketDto {
    private String bracket;     // e.g. "< 25 ans", "25 - 34 ans", "35 - 44 ans", "45 - 54 ans", "55+ ans"
    private long count;
    private double percentage;
}

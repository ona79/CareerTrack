package com.careertrack.modules.training.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingDto {
    private UUID id;
    private String code;
    private String title;
    private String description;
    private Integer durationHours;
    private String provider;
    private String category;
}

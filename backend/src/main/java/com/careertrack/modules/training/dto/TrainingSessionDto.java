package com.careertrack.modules.training.dto;

import com.careertrack.modules.training.entity.TrainingSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingSessionDto {
    private UUID id;
    private UUID trainingId;
    private String trainingTitle;
    private String trainingCode;
    private String sessionCode;
    private Instant startDate;
    private Instant endDate;
    private String location;
    private int maxParticipants;
    private int enrolledParticipantsCount;
    private TrainingSessionStatus status;
}

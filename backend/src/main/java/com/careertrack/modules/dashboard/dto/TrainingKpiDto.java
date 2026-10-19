package com.careertrack.modules.dashboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingKpiDto {
    private long totalTrainings;
    private long activeSessions;
    private long completedSessions;
    private long totalParticipants;
}

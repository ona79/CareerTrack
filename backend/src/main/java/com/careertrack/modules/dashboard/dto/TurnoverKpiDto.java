package com.careertrack.modules.dashboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoverKpiDto {
    private double turnoverRate;       // Percentage e.g. 5.2%
    private long hiresCount;           // Recrutements dans l'année
    private long departuresCount;      // Départs dans l'année
    private long averageHeadcount;     // Effectif moyen de référence
}

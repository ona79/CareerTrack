package com.careertrack.modules.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "objectives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Objective {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50, nullable = false)
    private ObjectiveCategory category;

    @Column(name = "weight", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal weight = BigDecimal.ONE;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "achievement_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal achievementRate = BigDecimal.ZERO;

    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private String status = "IN_PROGRESS";
}

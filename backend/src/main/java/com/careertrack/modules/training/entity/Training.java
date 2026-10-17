package com.careertrack.modules.training.entity;

import com.careertrack.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "trainings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Training extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "category", length = 100, nullable = false)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_hours", nullable = false)
    @Builder.Default
    private int durationHours = 0;

    @Column(name = "provider", length = 150)
    private String provider;
}

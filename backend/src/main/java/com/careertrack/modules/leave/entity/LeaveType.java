package com.careertrack.modules.leave.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "max_days_per_year")
    private Integer maxDaysPerYear;

    @Column(name = "is_paid", nullable = false)
    @Builder.Default
    private boolean paid = true;

    @Column(name = "requires_justification", nullable = false)
    @Builder.Default
    private boolean requiresJustification = false;
}

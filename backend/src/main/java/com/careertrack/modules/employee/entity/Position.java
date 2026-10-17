package com.careertrack.modules.employee.entity;

import com.careertrack.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", length = 30, nullable = false, unique = true)
    private String code;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "min_salary", precision = 14, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal minSalary = BigDecimal.ZERO;

    @Column(name = "max_salary", precision = 14, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal maxSalary = BigDecimal.ZERO;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}

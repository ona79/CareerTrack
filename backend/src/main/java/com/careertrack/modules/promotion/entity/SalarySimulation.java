package com.careertrack.modules.promotion.entity;

import com.careertrack.modules.auth.entity.User;
import com.careertrack.modules.employee.entity.Department;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "salary_simulations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalarySimulation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "average_increase_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal averageIncreasePercentage;

    @Column(name = "projected_payroll_impact", precision = 14, scale = 2, nullable = false)
    private BigDecimal projectedPayrollImpact;

    @Column(name = "simulated_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant simulatedAt = Instant.now();
}

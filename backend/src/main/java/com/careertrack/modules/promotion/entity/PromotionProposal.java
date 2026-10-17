package com.careertrack.modules.promotion.entity;

import com.careertrack.common.entity.AuditableEntity;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.Position;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promotion_proposals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionProposal extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_by")
    private Employee proposedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_position_id")
    private Position targetPosition;

    @Column(name = "current_salary", precision = 14, scale = 2, nullable = false)
    private BigDecimal currentSalary;

    @Column(name = "proposed_salary", precision = 14, scale = 2, nullable = false)
    private BigDecimal proposedSalary;

    @Column(name = "justification", columnDefinition = "TEXT", nullable = false)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private PromotionStatus status = PromotionStatus.SUBMITTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private Employee validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;
}

package com.careertrack.modules.evaluation.entity;

import com.careertrack.common.entity.AuditableEntity;
import com.careertrack.modules.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private EvaluationCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id")
    private Employee evaluator;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private EvaluationStatus status = EvaluationStatus.DRAFT;

    @Column(name = "self_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal selfScore = BigDecimal.ZERO;

    @Column(name = "manager_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal managerScore = BigDecimal.ZERO;

    @Column(name = "final_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal finalScore = BigDecimal.ZERO;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "summary_pdf_url", length = 500)
    private String summaryPdfUrl;

    @Column(name = "interview_date")
    private LocalDate interviewDate;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Objective> objectives = new ArrayList<>();
}

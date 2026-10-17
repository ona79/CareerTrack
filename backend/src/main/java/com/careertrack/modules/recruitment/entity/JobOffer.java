package com.careertrack.modules.recruitment.entity;

import com.careertrack.common.entity.AuditableEntity;
import com.careertrack.modules.employee.entity.ContractType;
import com.careertrack.modules.employee.entity.Department;
import com.careertrack.modules.employee.entity.Position;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "job_offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOffer extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", length = 30, nullable = false)
    private ContractType contractType;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private JobOfferStatus status = JobOfferStatus.OPEN;

    @Column(name = "open_positions", nullable = false)
    @Builder.Default
    private int openPositions = 1;

    @Column(name = "closing_date")
    private LocalDate closingDate;
}

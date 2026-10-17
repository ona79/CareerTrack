package com.careertrack.modules.recruitment.entity;

import com.careertrack.modules.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "application_interviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationInterview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id")
    private Employee interviewer;

    @Column(name = "interview_date", nullable = false)
    private Instant interviewDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", length = 50, nullable = false)
    private InterviewType interviewType;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "result", length = 30)
    private String result;
}

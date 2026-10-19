package com.smarthr.smarthr.evaluation.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;
import com.smarthr.smarthr.employee.entity.EmployeeDetails;


@Entity
@Table(name = "evaluations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The employee being evaluated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", referencedColumnName = "id", nullable = false)
    private EmployeeDetails employee;

    // The manager/reviewer conducting the evaluation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id", referencedColumnName = "id", nullable = false)
    private EmployeeDetails evaluator;

    // Start/end of the period this evaluation covers (e.g., a quarter)
    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private EvaluationStatus status = EvaluationStatus.DRAFT;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvaluationItemEntity> items = new ArrayList<>();

    @Column(name = "overall_comment", columnDefinition = "TEXT")
    private String overallComment;

    // Manager's recommendation, only set for end-of-probation evaluations
    @Enumerated(EnumType.STRING)
    @Column(name = "probation_recommendation")
    private ProbationRecommendation probationRecommendation;

    // HR's final decision on a probation evaluation
    @Enumerated(EnumType.STRING)
    @Column(name = "hr_decision")
    private ProbationRecommendation hrDecision;

    @Column(name = "hr_comment", columnDefinition = "TEXT")
    private String hrComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hr_decided_by", referencedColumnName = "id")
    private EmployeeDetails hrDecidedBy;

    @Column(name = "hr_decided_at")
    private LocalDateTime hrDecidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

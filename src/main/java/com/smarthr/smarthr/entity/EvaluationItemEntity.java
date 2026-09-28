package com.smarthr.smarthr.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evaluation_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", referencedColumnName = "id", nullable = false)
    private EvaluationEntity evaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criteria_id", referencedColumnName = "id", nullable = false)
    private EvaluationCriteriaEntity criteria;

    // 1-5 rating for this criterion
    @Column(name = "score", nullable = false)
    private Integer score;
}

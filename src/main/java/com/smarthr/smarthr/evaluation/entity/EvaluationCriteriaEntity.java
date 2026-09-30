package com.smarthr.smarthr.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evaluation_criteria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCriteriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // e.g., "Job Knowledge & Skills", "Quality of Work"
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    // e.g., "Understanding of role responsibilities, tools, and processes."
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Controls display order of the criteria list in the evaluation form
    @Column(name = "display_order")
    private Integer displayOrder;
}

package com.smarthr.smarthr.evaluation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class EvaluationResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long evaluatorId;
    private String evaluatorName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String status;
    private String overallComment;
    private Double overallScore;
    private List<EvaluationItemResponse> items;
    private LocalDateTime createdAt;
}

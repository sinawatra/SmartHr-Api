package com.smarthr.smarthr.evaluation.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.smarthr.smarthr.evaluation.entity.ProbationRecommendation;

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
public class SubmitEvaluationRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    private String overallComment;

    // PASS, FAIL or DELAY (case-insensitive); only for end-of-probation evaluations
    private ProbationRecommendation probationRecommendation;

    @NotEmpty(message = "At least one scored criterion is required")
    @Valid
    private List<EvaluationItemRequest> items;
}

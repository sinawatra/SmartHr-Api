package com.smarthr.smarthr.evaluation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import com.smarthr.smarthr.evaluation.entity.ProbationRecommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HR's final probation decision on a manager's evaluation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmProbationRequest {

    // PASS, FAIL or DELAY (case-insensitive); may differ from the manager's recommendation
    @NotNull(message = "Decision is required (PASS, FAIL or DELAY)")
    private ProbationRecommendation decision;

    private String comment;

    // Required for DELAY: the extended probation end date
    private LocalDate newProbationEndDate;

    // Optional for FAIL: the employee's last working day, defaults to today
    private LocalDate lastWorkingDate;
}

package com.smarthr.smarthr.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProbationSummaryResponse {
    /** Employees whose status is still Probation. */
    private long onProbation;
    /** Still on probation, end date between today and 14 days from today. */
    private long endingSoon;
    /** Now FullStaff, with a probation end date in the current quarter. */
    private long completedThisQuarter;
    /** Still on probation although the end date has passed, awaiting a decision. */
    private long pendingReview;
}

package com.smarthr.smarthr.evaluation.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * The evaluator's recommendation at the end of an employee's probation.
 */
public enum ProbationRecommendation {
    /** Confirm the employee. */
    PASS,
    /** End the employment. */
    FAIL,
    /** Extend the probation period. */
    DELAY;

    @JsonCreator
    public static ProbationRecommendation fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (ProbationRecommendation recommendation : ProbationRecommendation.values()) {
            if (recommendation.name().equalsIgnoreCase(trimmed)) {
                return recommendation;
            }
        }
        throw new IllegalArgumentException("Invalid probation recommendation '" + value + "', expected PASS, FAIL or DELAY");
    }
}

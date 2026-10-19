package com.smarthr.smarthr.evaluation.entity;

public enum EvaluationStatus {
    DRAFT,
    // Submitted by the manager; probation evaluations wait here for HR to confirm
    SUBMITTED,
    ACKNOWLEDGED,
    // HR has made the final probation decision
    CONFIRMED
}

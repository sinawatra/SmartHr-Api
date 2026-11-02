package com.smarthr.smarthr.notification.entity;

/**
 * What a notification is about. The frontend uses it together with
 * {@code referenceId} to decide which screen to open.
 */
public enum NotificationType {
    // referenceId: leave request ID
    LEAVE_REQUESTED,
    LEAVE_APPROVED,
    LEAVE_REJECTED,
    // referenceId: evaluation ID
    PROBATION_EVALUATION_SUBMITTED,
    PROBATION_DECIDED,
    // referenceId: employee ID
    PROBATION_ENDING_SOON,
    // referenceId: announcement ID
    ANNOUNCEMENT
}

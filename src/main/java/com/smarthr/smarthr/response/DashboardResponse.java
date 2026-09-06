package com.smarthr.smarthr.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated statistics for the HR dashboard overview cards.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    /** Total Employees card. */
    private long totalEmployees;
    private long activeEmployees;

    /** Present Today card. */
    private long presentToday;
    private double presentTodayRate;

    /** On Leave card. */
    private long onLeave;
    private long onLeavePending;

    /** Pending Approvals card. */
    private long pendingApprovals;

    /** Probation Ending card. */
    private long probationEnding;
    private double probationEndingRate;

    /** Attendance Rate card (live rate). */
    private double attendanceRate;
}

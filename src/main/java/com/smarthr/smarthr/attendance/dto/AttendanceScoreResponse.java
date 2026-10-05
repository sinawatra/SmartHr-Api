package com.smarthr.smarthr.attendance.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Attendance score for one employee over a period, with the breakdown behind it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceScoreResponse {
    private Long employeeId;
    private String employeeName;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    // 0-100
    private int score;
    // 1-5, suggested value for the "Attendance & Punctuality" evaluation criterion
    private int suggestedRating;

    private int workingDays;
    private int presentDays;
    private int lateDays;
    private int earlyLeaveDays;
    private int missedClockOutDays;
    private int absentDays;
    private int leaveDays;
    private long totalLateMinutes;

    // Percentages, rounded to one decimal place
    private double attendanceRate;
    private double punctualityRate;

    private int lateDeduction;
    private int earlyLeaveDeduction;
    private int missedClockOutDeduction;
    private int absenceDeduction;
    private int bonus;
}

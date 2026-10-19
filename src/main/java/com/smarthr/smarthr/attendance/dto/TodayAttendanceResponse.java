package com.smarthr.smarthr.attendance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An employee's attendance for today, for the bot and web app to show clock-in/out state.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodayAttendanceResponse {

    /** What the employee's day looks like right now. */
    public enum Status {
        /** Working day, no clock-in yet. */
        NOT_CLOCKED_IN,
        /** Clocked in today and not clocked out yet. */
        CLOCKED_IN,
        /** Clocked in and out today. */
        CLOCKED_OUT,
        /** No clock-in, on approved leave today. */
        ON_LEAVE,
        /** No clock-in, today is not a working day. */
        DAY_OFF
    }

    private Long employeeId;
    private String employeeName;
    private LocalDate date;
    private Status status;

    private boolean workingDay;
    private boolean onLeave;
    private LocalTime scheduledStart;
    private LocalTime scheduledEnd;

    // Null until the employee clocks in
    private Long attendanceId;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private boolean late;
    private Long lateMinutes;
    private Long earlyLeaveMinutes;
    private Long overtimeMinutes;
    private Long workDurationMinutes;
}

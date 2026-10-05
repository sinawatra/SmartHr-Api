package com.smarthr.smarthr.company.entity;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A company's working hours, with defaults for anything the company has not configured.
 *
 * <p>Lateness is counted from the scheduled start time once the clock-in passes
 * the grace period (e.g. start 08:00, grace 10 → clocking in at 08:25 is 25 minutes late,
 * clocking in at 08:09 is on time).
 */
public record WorkSchedule(LocalTime startTime, LocalTime endTime, int lateGraceMinutes, Set<DayOfWeek> workingDays) {

    public static final LocalTime DEFAULT_START_TIME = LocalTime.of(8, 0);
    public static final LocalTime DEFAULT_END_TIME = LocalTime.of(17, 0);
    public static final int DEFAULT_LATE_GRACE_MINUTES = 10;
    public static final Set<DayOfWeek> DEFAULT_WORKING_DAYS = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

    public static WorkSchedule defaults() {
        return new WorkSchedule(DEFAULT_START_TIME, DEFAULT_END_TIME, DEFAULT_LATE_GRACE_MINUTES, DEFAULT_WORKING_DAYS);
    }

    /** Schedule for the given company; a null company or unset fields fall back to the defaults. */
    public static WorkSchedule of(CompanyEntity company) {
        if (company == null) {
            return defaults();
        }
        return new WorkSchedule(
                company.getWorkStartTime() != null ? company.getWorkStartTime() : DEFAULT_START_TIME,
                company.getWorkEndTime() != null ? company.getWorkEndTime() : DEFAULT_END_TIME,
                company.getLateGraceMinutes() != null ? company.getLateGraceMinutes() : DEFAULT_LATE_GRACE_MINUTES,
                parseWorkingDays(company.getWorkingDays()));
    }

    /** Parses a comma-separated list such as "MONDAY,TUESDAY"; blank means the default Mon–Fri. */
    public static Set<DayOfWeek> parseWorkingDays(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_WORKING_DAYS;
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> DayOfWeek.valueOf(s.toUpperCase()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    public static String formatWorkingDays(Set<DayOfWeek> days) {
        return days.stream().sorted().map(DayOfWeek::name).collect(Collectors.joining(","));
    }

    public boolean isWorkingDay(LocalDate date) {
        return workingDays.contains(date.getDayOfWeek());
    }

    /** Minutes late for a clock-in, or 0 when on time, within the grace period, or on a non-working day. */
    public long lateMinutes(LocalDateTime clockIn) {
        if (!isWorkingDay(clockIn.toLocalDate())) {
            return 0;
        }
        LocalDateTime scheduledStart = clockIn.toLocalDate().atTime(startTime);
        if (!clockIn.isAfter(scheduledStart.plusMinutes(lateGraceMinutes))) {
            return 0;
        }
        return Duration.between(scheduledStart, clockIn).toMinutes();
    }

    /** Minutes left before the scheduled end, for a same-day clock-out on a working day; otherwise 0. */
    public long earlyLeaveMinutes(LocalDateTime clockIn, LocalDateTime clockOut) {
        if (!isWorkingDay(clockIn.toLocalDate()) || !clockOut.toLocalDate().equals(clockIn.toLocalDate())) {
            return 0;
        }
        LocalDateTime scheduledEnd = clockIn.toLocalDate().atTime(endTime);
        return clockOut.isBefore(scheduledEnd) ? Duration.between(clockOut, scheduledEnd).toMinutes() : 0;
    }

    /**
     * Minutes worked past the scheduled end on a working day, or the whole session on a non-working day.
     * A clock-out on a later date is a forgotten clock-out, not overtime, so it counts as 0.
     */
    public long overtimeMinutes(LocalDateTime clockIn, LocalDateTime clockOut) {
        if (!clockOut.toLocalDate().equals(clockIn.toLocalDate())) {
            return 0;
        }
        if (!isWorkingDay(clockIn.toLocalDate())) {
            return Duration.between(clockIn, clockOut).toMinutes();
        }
        LocalDateTime scheduledEnd = clockIn.toLocalDate().atTime(endTime);
        LocalDateTime overtimeStart = clockIn.isAfter(scheduledEnd) ? clockIn : scheduledEnd;
        return clockOut.isAfter(overtimeStart) ? Duration.between(overtimeStart, clockOut).toMinutes() : 0;
    }
}

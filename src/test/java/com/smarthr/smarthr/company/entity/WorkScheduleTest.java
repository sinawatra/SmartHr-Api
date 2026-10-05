package com.smarthr.smarthr.company.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class WorkScheduleTest {

    // 2026-10-05 is a Monday
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 10);

    private final WorkSchedule schedule = WorkSchedule.defaults();

    @Test
    void lateMinutes_WithinGracePeriod_IsZero() {
        assertEquals(0, schedule.lateMinutes(MONDAY.atTime(8, 10)));
    }

    @Test
    void lateMinutes_AfterGracePeriod_CountsFromScheduledStart() {
        assertEquals(11, schedule.lateMinutes(MONDAY.atTime(8, 11)));
    }

    @Test
    void lateMinutes_OnNonWorkingDay_IsZero() {
        assertEquals(0, schedule.lateMinutes(SATURDAY.atTime(11, 0)));
    }

    @Test
    void earlyLeaveMinutes_BeforeScheduledEnd() {
        assertEquals(30, schedule.earlyLeaveMinutes(MONDAY.atTime(8, 0), MONDAY.atTime(16, 30)));
        assertEquals(0, schedule.earlyLeaveMinutes(MONDAY.atTime(8, 0), MONDAY.atTime(17, 0)));
    }

    @Test
    void earlyLeaveAndOvertime_ClockOutOnLaterDay_AreZero() {
        LocalDateTime nextDay = MONDAY.plusDays(1).atTime(9, 0);
        assertEquals(0, schedule.earlyLeaveMinutes(MONDAY.atTime(8, 0), nextDay));
        assertEquals(0, schedule.overtimeMinutes(MONDAY.atTime(8, 0), nextDay));
    }

    @Test
    void overtimeMinutes_AfterScheduledEnd_AndWholeSessionOnNonWorkingDay() {
        assertEquals(45, schedule.overtimeMinutes(MONDAY.atTime(8, 0), MONDAY.atTime(17, 45)));
        assertEquals(120, schedule.overtimeMinutes(SATURDAY.atTime(9, 0), SATURDAY.atTime(11, 0)));
    }

    @Test
    void of_UsesCompanySettingsAndFallsBackToDefaults() {
        CompanyEntity company = CompanyEntity.builder()
                .workStartTime(LocalTime.of(9, 0))
                .workingDays("monday, saturday")
                .build();

        WorkSchedule custom = WorkSchedule.of(company);

        assertEquals(LocalTime.of(9, 0), custom.startTime());
        assertEquals(WorkSchedule.DEFAULT_END_TIME, custom.endTime());
        assertEquals(WorkSchedule.DEFAULT_LATE_GRACE_MINUTES, custom.lateGraceMinutes());
        assertEquals(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY), custom.workingDays());
        assertEquals(WorkSchedule.defaults(), WorkSchedule.of(null));
    }
}

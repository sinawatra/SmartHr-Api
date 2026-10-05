package com.smarthr.smarthr.attendance.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.attendance.dto.AttendanceScoreResponse;
import com.smarthr.smarthr.attendance.entity.AttendanceEntity;
import com.smarthr.smarthr.attendance.repository.AttendanceRepository;
import com.smarthr.smarthr.common.dto.PagedResponse;
import com.smarthr.smarthr.company.entity.WorkSchedule;
import com.smarthr.smarthr.company.service.WorkScheduleService;
import com.smarthr.smarthr.employee.entity.EmployeeDetails;
import com.smarthr.smarthr.employee.entity.EmployementStatus;
import com.smarthr.smarthr.employee.repository.EmployeeRepository;
import com.smarthr.smarthr.leave.entity.LeaveRequestEntity;
import com.smarthr.smarthr.leave.entity.LeaveStatus;
import com.smarthr.smarthr.leave.repository.LeaveRequestRepository;

import lombok.RequiredArgsConstructor;

/**
 * Calculates an employee's attendance score for a period from their clock-ins,
 * clock-outs and approved leave.
 *
 * <p>Every employee starts at 100. For each working day in the period:
 * <ul>
 *   <li>late by less than 30 minutes: -1; 30 minutes or more: -2</li>
 *   <li>left before the scheduled end: -1</li>
 *   <li>forgot to clock out: -2</li>
 *   <li>no clock-in and no approved leave: -5 (absent)</li>
 *   <li>approved leave: no deduction</li>
 * </ul>
 * A period with no late days and no absences earns a +5 bonus. The score is kept between 0 and 100.
 *
 * <p>Today is only counted once the employee has clocked in, since the day is not over yet.
 * Public holidays are not modelled, so a holiday on a working day counts as an absence.
 */
@Service
@RequiredArgsConstructor
public class AttendanceScoreService {

    private static final int BASE_SCORE = 100;
    private static final int LATE_DEDUCTION = 1;
    private static final int VERY_LATE_DEDUCTION = 2;
    private static final long VERY_LATE_THRESHOLD_MINUTES = 30;
    private static final int EARLY_LEAVE_DEDUCTION = 1;
    private static final int MISSED_CLOCK_OUT_DEDUCTION = 2;
    private static final int ABSENCE_DEDUCTION = 5;
    private static final int PERFECT_ATTENDANCE_BONUS = 5;

    private static final Set<EmployementStatus> INACTIVE_STATUSES =
            EnumSet.of(EmployementStatus.Resigned, EmployementStatus.Terminated);

    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkScheduleService workScheduleService;
    private final Clock clock;

    /**
     * Score for one employee. Allowed for ADMIN and HR, the employee's line manager, and the employee themselves.
     */
    @Transactional(readOnly = true)
    public AttendanceScoreResponse getScore(Long employeeId, LocalDate from, LocalDate to) {
        EmployeeDetails employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + employeeId));
        checkCanView(employee);
        return scoreFor(employee, from, to);
    }

    /** Score for the currently authenticated employee. */
    @Transactional(readOnly = true)
    public AttendanceScoreResponse getMyScore(LocalDate from, LocalDate to) {
        return scoreFor(currentEmployee(), from, to);
    }

    /**
     * Scores for every active employee for a month, highest first.
     * Attendance and leave are loaded once for the whole month rather than per employee.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AttendanceScoreResponse> getScores(YearMonth month, Pageable pageable) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        Map<Long, List<AttendanceEntity>> attendanceByEmployee = attendanceRepository
                .findByClockInBetweenOrderByClockInDesc(from.atStartOfDay(), to.atTime(23, 59, 59))
                .stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId()));
        Map<Long, List<LeaveRequestEntity>> leavesByEmployee = leaveRequestRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(LeaveStatus.APPROVED, to, from)
                .stream()
                .collect(Collectors.groupingBy(l -> l.getEmployee().getId()));

        List<AttendanceScoreResponse> scores = employeeRepository.findAll().stream()
                .filter(e -> e.getEmployeeStatus() == null || !INACTIVE_STATUSES.contains(e.getEmployeeStatus()))
                .map(e -> calculate(e, from, to,
                        attendanceByEmployee.getOrDefault(e.getId(), List.of()),
                        leavesByEmployee.getOrDefault(e.getId(), List.of())))
                .sorted(Comparator.comparingInt(AttendanceScoreResponse::getScore).reversed()
                        .thenComparing(AttendanceScoreResponse::getEmployeeName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        int fromIndex = (int) Math.min(pageable.getOffset(), scores.size());
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), scores.size());
        return PagedResponse.of(new PageImpl<>(scores.subList(fromIndex, toIndex), pageable, scores.size()));
    }

    private AttendanceScoreResponse scoreFor(EmployeeDetails employee, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("End date must be on or after the start date.");
        }
        List<AttendanceEntity> records = attendanceRepository.findByEmployeeIdAndClockInBetweenOrderByClockInDesc(
                employee.getId(), from.atStartOfDay(), to.atTime(23, 59, 59));
        List<LeaveRequestEntity> leaves = leaveRequestRepository
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employee.getId(), LeaveStatus.APPROVED, to, from);
        return calculate(employee, from, to, records, leaves);
    }

    private AttendanceScoreResponse calculate(EmployeeDetails employee, LocalDate from, LocalDate to,
            List<AttendanceEntity> records, List<LeaveRequestEntity> approvedLeaves) {
        WorkSchedule schedule = workScheduleService.forEmployee(employee);
        LocalDate today = LocalDate.now(clock);

        // Only score days the employee was actually employed, and nothing in the future.
        LocalDate start = from;
        if (employee.getHiredate() != null && employee.getHiredate().isAfter(start)) {
            start = employee.getHiredate();
        }
        LocalDate end = to.isAfter(today) ? today : to;
        if (employee.getEndDate() != null && employee.getEndDate().isBefore(end)) {
            end = employee.getEndDate();
        }

        // First clock-in of each day
        Map<LocalDate, AttendanceEntity> attendanceByDay = new HashMap<>();
        for (AttendanceEntity record : records) {
            attendanceByDay.merge(record.getClockIn().toLocalDate(), record,
                    (a, b) -> a.getClockIn().isBefore(b.getClockIn()) ? a : b);
        }

        Set<LocalDate> leaveDates = new HashSet<>();
        for (LeaveRequestEntity leave : approvedLeaves) {
            for (LocalDate d = leave.getStartDate(); !d.isAfter(leave.getEndDate()); d = d.plusDays(1)) {
                leaveDates.add(d);
            }
        }

        int workingDays = 0, presentDays = 0, lateDays = 0, earlyLeaveDays = 0;
        int missedClockOutDays = 0, absentDays = 0, leaveDays = 0;
        long totalLateMinutes = 0;
        int lateDeduction = 0, earlyLeaveDeduction = 0, missedClockOutDeduction = 0, absenceDeduction = 0;

        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            if (!schedule.isWorkingDay(day)) {
                continue;
            }
            AttendanceEntity attendance = attendanceByDay.get(day);
            if (attendance == null && day.equals(today)) {
                continue;
            }
            workingDays++;

            if (attendance == null) {
                if (leaveDates.contains(day)) {
                    leaveDays++;
                } else {
                    absentDays++;
                    absenceDeduction += ABSENCE_DEDUCTION;
                }
                continue;
            }

            presentDays++;
            // Clocked in on a leave day (e.g. half-day leave): count the presence, skip punctuality penalties.
            if (leaveDates.contains(day)) {
                continue;
            }

            long lateMinutes = resolveLateMinutes(attendance, schedule);
            if (lateMinutes > 0) {
                lateDays++;
                totalLateMinutes += lateMinutes;
                lateDeduction += lateMinutes >= VERY_LATE_THRESHOLD_MINUTES ? VERY_LATE_DEDUCTION : LATE_DEDUCTION;
            }

            if (resolveEarlyLeaveMinutes(attendance, schedule) > 0) {
                earlyLeaveDays++;
                earlyLeaveDeduction += EARLY_LEAVE_DEDUCTION;
            }

            if (isMissedClockOut(attendance, today)) {
                missedClockOutDays++;
                missedClockOutDeduction += MISSED_CLOCK_OUT_DEDUCTION;
            }
        }

        int bonus = workingDays > 0 && lateDays == 0 && absentDays == 0 ? PERFECT_ATTENDANCE_BONUS : 0;
        int rawScore = BASE_SCORE - lateDeduction - earlyLeaveDeduction - missedClockOutDeduction
                - absenceDeduction + bonus;
        int score = Math.max(0, Math.min(BASE_SCORE, rawScore));

        int expectedDays = workingDays - leaveDays;
        double attendanceRate = expectedDays > 0 ? percentage(presentDays, expectedDays) : 100.0;
        double punctualityRate = presentDays > 0 ? percentage(presentDays - lateDays, presentDays) : 100.0;

        return AttendanceScoreResponse.builder()
                .employeeId(employee.getId())
                .employeeName(displayName(employee))
                .periodStart(from)
                .periodEnd(to)
                .score(score)
                .suggestedRating(toRating(score))
                .workingDays(workingDays)
                .presentDays(presentDays)
                .lateDays(lateDays)
                .earlyLeaveDays(earlyLeaveDays)
                .missedClockOutDays(missedClockOutDays)
                .absentDays(absentDays)
                .leaveDays(leaveDays)
                .totalLateMinutes(totalLateMinutes)
                .attendanceRate(attendanceRate)
                .punctualityRate(punctualityRate)
                .lateDeduction(lateDeduction)
                .earlyLeaveDeduction(earlyLeaveDeduction)
                .missedClockOutDeduction(missedClockOutDeduction)
                .absenceDeduction(absenceDeduction)
                .bonus(bonus)
                .build();
    }

    /** Stored value when present; records created before late tracking existed are recalculated. */
    private long resolveLateMinutes(AttendanceEntity attendance, WorkSchedule schedule) {
        return attendance.getLateMinutes() != null
                ? attendance.getLateMinutes()
                : schedule.lateMinutes(attendance.getClockIn());
    }

    private long resolveEarlyLeaveMinutes(AttendanceEntity attendance, WorkSchedule schedule) {
        if (attendance.getEarlyLeaveMinutes() != null) {
            return attendance.getEarlyLeaveMinutes();
        }
        return attendance.getClockOut() != null
                ? schedule.earlyLeaveMinutes(attendance.getClockIn(), attendance.getClockOut())
                : 0;
    }

    /** Still open after the day ended, or only closed on a later day. */
    private boolean isMissedClockOut(AttendanceEntity attendance, LocalDate today) {
        LocalDate day = attendance.getClockIn().toLocalDate();
        LocalDateTime clockOut = attendance.getClockOut();
        return clockOut == null ? day.isBefore(today) : clockOut.toLocalDate().isAfter(day);
    }

    /** Maps a 0-100 score onto the 1-5 scale used by evaluation criteria. */
    static int toRating(int score) {
        if (score >= 90) return 5;
        if (score >= 80) return 4;
        if (score >= 70) return 3;
        if (score >= 60) return 2;
        return 1;
    }

    private static double percentage(int part, int whole) {
        return Math.round(part * 1000.0 / whole) / 10.0;
    }

    private void checkCanView(EmployeeDetails employee) {
        if (hasAuthority("ADMIN") || hasAuthority("HR")) {
            return;
        }
        EmployeeDetails current = currentEmployee();
        boolean isSelf = current.getId().equals(employee.getId());
        boolean isDirectManager = employee.getManagerId() != null
                && Long.valueOf(employee.getManagerId().longValue()).equals(current.getId());
        if (!isSelf && !isDirectManager) {
            throw new AccessDeniedException("You are not allowed to view this employee's attendance score.");
        }
    }

    private String displayName(EmployeeDetails employee) {
        String first = employee.getFirstName();
        String last = employee.getLastName();
        if (first != null && last != null) {
            return (first + " " + last).trim();
        }
        if (first != null) {
            return first;
        }
        return employee.getUsername();
    }

    /** The employee behind the current authentication, or an error if none. */
    private EmployeeDetails currentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("No authenticated user.");
        }
        String username = auth.getName();
        return employeeRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found for authenticated user: " + username));
    }

    /** Whether the current authentication carries the given authority. */
    private boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority granted : auth.getAuthorities()) {
            if (authority.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}

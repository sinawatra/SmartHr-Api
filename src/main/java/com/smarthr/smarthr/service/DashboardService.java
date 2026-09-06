package com.smarthr.smarthr.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.enumeration.EmployementStatus;
import com.smarthr.smarthr.enumeration.LeaveStatus;
import com.smarthr.smarthr.repository.DashboardRepository;
import com.smarthr.smarthr.response.DashboardResponse;

import lombok.RequiredArgsConstructor;

/**
 * Builds the aggregated statistics shown on the HR dashboard.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Set<EmployementStatus> INACTIVE_STATUSES =
            Set.of(EmployementStatus.Resigned, EmployementStatus.Terminated);

    /** How many days ahead counts as "probation ending soon". */
    private static final int PROBATION_WINDOW_DAYS = 30;

    private final DashboardRepository dashboardRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        long totalEmployees = dashboardRepository.countTotalEmployees();
        long activeEmployees = dashboardRepository.countActiveEmployees(INACTIVE_STATUSES);
        long presentToday = dashboardRepository.countPresentBetween(startOfDay, endOfDay);
        long onLeave = dashboardRepository.countOnLeaveOn(LeaveStatus.APPROVED, today);
        long onLeavePending = dashboardRepository.countLeaveRequestsByStatus(LeaveStatus.PENDING);
        long pendingApprovals = onLeavePending;
        long probationEnding = dashboardRepository.countProbationEndingBetween(
                today, today.plusDays(PROBATION_WINDOW_DAYS));

        double attendanceRate = percentage(presentToday, activeEmployees);
        double probationEndingRate = percentage(probationEnding, activeEmployees);

        return DashboardResponse.builder()
                .totalEmployees(totalEmployees)
                .activeEmployees(activeEmployees)
                .presentToday(presentToday)
                .presentTodayRate(attendanceRate)
                .onLeave(onLeave)
                .onLeavePending(onLeavePending)
                .pendingApprovals(pendingApprovals)
                .probationEnding(probationEnding)
                .probationEndingRate(probationEndingRate)
                .attendanceRate(attendanceRate)
                .build();
    }

    /** Percentage of {@code part} out of {@code whole}, rounded to two decimals; 0 when {@code whole} is 0. */
    private double percentage(long part, long whole) {
        if (whole <= 0) {
            return 0.0;
        }
        return Math.round((part * 10000.0) / whole) / 100.0;
    }
}

package com.smarthr.smarthr.notification.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.employee.entity.EmployeeDetails;
import com.smarthr.smarthr.employee.entity.EmployementStatus;
import com.smarthr.smarthr.employee.repository.EmployeeRepository;
import com.smarthr.smarthr.notification.entity.NotificationType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Daily job that tells the line manager and HR when an employee's probation
 * ends in {@code notification.probation-reminder.days-ahead} days, so the
 * evaluation can be done in time.
 *
 * <p>It only matches the exact end date, so each employee is reminded once per
 * probation end date (a DELAY decision moves the date and triggers a new reminder).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProbationReminderJob {

    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Value("${notification.probation-reminder.days-ahead:7}")
    private int daysAhead;

    @Scheduled(cron = "${notification.probation-reminder.cron:0 0 8 * * *}")
    @Transactional
    public void sendProbationEndingReminders() {
        LocalDate endDate = LocalDate.now(clock).plusDays(daysAhead);
        List<EmployeeDetails> employees = employeeRepository
                .findByEmployeeStatusAndProbationEndDate(EmployementStatus.Probation, endDate);

        for (EmployeeDetails employee : employees) {
            String title = "Probation ending soon";
            String message = displayName(employee) + "'s probation ends on " + endDate
                    + ". Please complete their probation evaluation.";
            notificationService.notifyManagerOf(employee, NotificationType.PROBATION_ENDING_SOON, title, message, employee.getId());
            notificationService.notifyRole("HR", employee.getId(), NotificationType.PROBATION_ENDING_SOON, title, message, employee.getId());
        }
        log.info("Sent probation-ending reminders for {} employee(s) ending on {}", employees.size(), endDate);
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
}

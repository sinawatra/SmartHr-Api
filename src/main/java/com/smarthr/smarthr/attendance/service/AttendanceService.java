package com.smarthr.smarthr.attendance.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.attendance.entity.AttendanceEntity;
import com.smarthr.smarthr.employee.entity.EmployeeDetails;
import com.smarthr.smarthr.attendance.repository.AttendanceRepository;
import com.smarthr.smarthr.employee.repository.EmployeeRepository;
import com.smarthr.smarthr.attendance.dto.ClockInRequest;
import com.smarthr.smarthr.attendance.dto.ClockOutRequest;
import com.smarthr.smarthr.attendance.dto.AttendanceResponse;
import com.smarthr.smarthr.common.dto.PagedResponse;

/**
 * Service class handling attendance business logic.
 *
 * @author sinawatrarith
 */
@Service
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceService(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Clock In an employee
     */
    @Transactional
    public AttendanceResponse clockIn(ClockInRequest request) {
        EmployeeDetails employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + request.getEmployeeId()));

        LocalDate today = LocalDate.now();

        // Check if employee already has an unclosed session from TODAY (clocked in without clocking out).
        // A session left open from a previous day is a missed clock-out; it stays open as-is
        // and does not block a fresh clock-in today.
        Optional<AttendanceEntity> activeSession = attendanceRepository
                .findTopByEmployeeIdAndClockOutIsNullOrderByClockInDesc(employee.getId());

        if (activeSession.isPresent() && !activeSession.get().getClockIn().toLocalDate().isBefore(today)) {
            throw new IllegalStateException("Already clocked in at " + activeSession.get().getClockIn());
        }

        // Only one clock-in allowed per day
        boolean alreadyClockedInToday = attendanceRepository.existsByEmployeeIdAndClockInBetween(
                employee.getId(), today.atStartOfDay(), today.atTime(23, 59, 59));

        if (alreadyClockedInToday) {
            throw new IllegalStateException("Employee has already clocked in today");
        }

        AttendanceEntity attendance = new AttendanceEntity();
        attendance.setEmployee(employee);
        attendance.setClockIn(LocalDateTime.now());
        attendance.setStatus("PRESENT");
        if (request.getNotes() != null) attendance.setNotes(request.getNotes());

        AttendanceEntity saved = attendanceRepository.save(attendance);
        return mapToResponse(saved);
    }

    /**
     * Clock Out an employee
     */
    @Transactional
    public AttendanceResponse clockOut(ClockOutRequest request) {
        AttendanceEntity attendance = attendanceRepository
                .findTopByEmployeeIdAndClockOutIsNullOrderByClockInDesc(request.getEmployeeId())
                .orElseThrow(() -> new IllegalStateException("No active clock-in record found for employee ID: " + request.getEmployeeId() + "Please go Clock In First befoer you can go Clock Out"));

        LocalDateTime now = LocalDateTime.now();
        attendance.setClockOut(now);

        // Calculate total shift duration in minutes
        long durationMinutes = Duration.between(attendance.getClockIn(), now).toMinutes();
        attendance.setWorkDurationMinutes(durationMinutes);
        attendance.setStatus("COMPLETED");

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            String updatedNotes = attendance.getNotes() != null 
                    ? " | Clock-in note: " + attendance.getNotes() + " | Clock-out note: " + request.getNotes() 
                    : request.getNotes();
            attendance.setNotes(updatedNotes);
        }

        AttendanceEntity updated = attendanceRepository.save(attendance);
        return mapToResponse(updated);
    }

    /**
     * Get active clock-in status for an employee
     */
    @Transactional(readOnly = true)
    public Optional<AttendanceResponse> getActiveSession(Long employeeId) {
        return attendanceRepository
                .findTopByEmployeeIdAndClockOutIsNullOrderByClockInDesc(employeeId)
                .map(this::mapToResponse);
    }

    /**
     * Get attendance history for an employee within a date range
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceHistory(Long employeeId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        return attendanceRepository.findByEmployeeIdAndClockInBetweenOrderByClockInDesc(employeeId, startDateTime, endDateTime)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get attendance history for an employee within a date range with pagination
     */
    @Transactional(readOnly = true)
    public PagedResponse<AttendanceResponse> getAttendanceHistory(Long employeeId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        Page<AttendanceEntity> page = attendanceRepository.findByEmployeeIdAndClockInBetween(
                employeeId, startDateTime, endDateTime, pageable);

        List<AttendanceResponse> content = page.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.of(page, content);
    }

    // Helper mapper method
    private AttendanceResponse mapToResponse(AttendanceEntity entity) {
        AttendanceResponse res = new AttendanceResponse();
        res.setId(entity.getId());
        res.setEmployeeId(entity.getEmployee().getId());
        res.setEmployeeName(entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName());
        res.setClockIn(entity.getClockIn());
        res.setClockOut(entity.getClockOut());
        res.setWorkDurationMinutes(entity.getWorkDurationMinutes());
        res.setStatus(entity.getStatus());
        res.setNotes(entity.getNotes());
        return res;
    }
}

package com.smarthr.smarthr.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.entity.AttendanceEntity;
import com.smarthr.smarthr.entity.EmployeeDetails;
import com.smarthr.smarthr.repository.AttendanceRepository;
import com.smarthr.smarthr.repository.EmployeeRepository;
import com.smarthr.smarthr.request.ClockInRequest;
import com.smarthr.smarthr.request.ClockOutRequest;
import com.smarthr.smarthr.response.AttendanceResponse;

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

        // Check if employee already has an unclosed session (clocked in without clocking out)
        Optional<AttendanceEntity> activeSession = attendanceRepository
                .findTopByEmployeeIdAndClockOutIsNullOrderByClockInDesc(employee.getId());

        if (activeSession.isPresent()) {
            throw new IllegalStateException("Already clocked in at " + activeSession.get().getClockIn());
        }

        AttendanceEntity attendance = new AttendanceEntity();
        attendance.setEmployee(employee);
        attendance.setClockIn(LocalDateTime.now());
        attendance.setStatus("PRESENT");
        
        if (request.getNotes() != null) {
            attendance.setNotes(request.getNotes());
        }

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
                .orElseThrow(() -> new IllegalStateException("No active clock-in record found for employee ID: " + request.getEmployeeId()));

        LocalDateTime now = LocalDateTime.now();
        attendance.setClockOut(now);

        // Calculate total shift duration in minutes
        long durationMinutes = Duration.between(attendance.getClockIn(), now).toMinutes();
        attendance.setWorkDurationMinutes(durationMinutes);
        attendance.setStatus("COMPLETED");

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            String updatedNotes = attendance.getNotes() != null 
                    ? attendance.getNotes() + " | Clock-out note: " + request.getNotes() 
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

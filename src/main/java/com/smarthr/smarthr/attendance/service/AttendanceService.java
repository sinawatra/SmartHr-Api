package com.smarthr.smarthr.attendance.service;

import java.time.Clock;
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
import com.smarthr.smarthr.attendance.dto.TodayAttendanceResponse;
import com.smarthr.smarthr.common.dto.PagedResponse;
import com.smarthr.smarthr.company.entity.WorkSchedule;
import com.smarthr.smarthr.company.service.WorkScheduleService;
import com.smarthr.smarthr.leave.entity.LeaveStatus;
import com.smarthr.smarthr.leave.repository.LeaveRequestRepository;

/**
 * Service class handling attendance business logic.
 *
 * @author sinawatrarith
 */
@Service
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkScheduleService workScheduleService;
    private final LeaveRequestRepository leaveRequestRepository;
    private final Clock clock;

    public AttendanceService(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository,
            WorkScheduleService workScheduleService, LeaveRequestRepository leaveRequestRepository, Clock clock) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.workScheduleService = workScheduleService;
        this.leaveRequestRepository = leaveRequestRepository;
        this.clock = clock;
    }

    /**
     * Clock In an employee
     */
    @Transactional
    public AttendanceResponse clockIn(ClockInRequest request) {
        EmployeeDetails employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + request.getEmployeeId()));

        LocalDate today = LocalDate.now(clock);

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

        LocalDateTime now = LocalDateTime.now(clock);
        long lateMinutes = workScheduleService.forEmployee(employee).lateMinutes(now);

        AttendanceEntity attendance = new AttendanceEntity();
        attendance.setEmployee(employee);
        attendance.setClockIn(now);
        attendance.setLateMinutes(lateMinutes);
        attendance.setStatus(lateMinutes > 0 ? "LATE" : "PRESENT");
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

        LocalDateTime now = LocalDateTime.now(clock);
        attendance.setClockOut(now);

        // Calculate total shift duration in minutes
        long durationMinutes = Duration.between(attendance.getClockIn(), now).toMinutes();
        attendance.setWorkDurationMinutes(durationMinutes);

        WorkSchedule schedule = workScheduleService.forEmployee(attendance.getEmployee());
        attendance.setEarlyLeaveMinutes(schedule.earlyLeaveMinutes(attendance.getClockIn(), now));
        attendance.setOvertimeMinutes(schedule.overtimeMinutes(attendance.getClockIn(), now));
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
     * Today's attendance status and clock-in/out times for an employee
     */
    @Transactional(readOnly = true)
    public TodayAttendanceResponse getTodayStatus(Long employeeId) {
        EmployeeDetails employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + employeeId));

        LocalDate today = LocalDate.now(clock);
        WorkSchedule schedule = workScheduleService.forEmployee(employee);
        boolean workingDay = schedule.isWorkingDay(today);
        boolean onLeave = !leaveRequestRepository
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employeeId, LeaveStatus.APPROVED, today, today)
                .isEmpty();

        // Only one clock-in is allowed per day, so the earliest record is today's
        Optional<AttendanceEntity> attendance = attendanceRepository
                .findByEmployeeIdAndClockInBetweenOrderByClockInDesc(employeeId, today.atStartOfDay(), today.atTime(23, 59, 59))
                .stream()
                .reduce((newer, older) -> older);

        TodayAttendanceResponse.TodayAttendanceResponseBuilder res = TodayAttendanceResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .date(today)
                .workingDay(workingDay)
                .onLeave(onLeave)
                .scheduledStart(schedule.startTime())
                .scheduledEnd(schedule.endTime());

        if (attendance.isEmpty()) {
            TodayAttendanceResponse.Status status = onLeave ? TodayAttendanceResponse.Status.ON_LEAVE
                    : !workingDay ? TodayAttendanceResponse.Status.DAY_OFF
                    : TodayAttendanceResponse.Status.NOT_CLOCKED_IN;
            return res.status(status).build();
        }

        AttendanceEntity record = attendance.get();
        // Records created before late tracking existed have no stored value
        long lateMinutes = record.getLateMinutes() != null ? record.getLateMinutes() : schedule.lateMinutes(record.getClockIn());
        Long workDurationMinutes = record.getClockOut() != null
                ? record.getWorkDurationMinutes()
                : Duration.between(record.getClockIn(), LocalDateTime.now(clock)).toMinutes();

        return res
                .status(record.getClockOut() == null ? TodayAttendanceResponse.Status.CLOCKED_IN : TodayAttendanceResponse.Status.CLOCKED_OUT)
                .attendanceId(record.getId())
                .clockIn(record.getClockIn())
                .clockOut(record.getClockOut())
                .late(lateMinutes > 0)
                .lateMinutes(lateMinutes)
                .earlyLeaveMinutes(record.getEarlyLeaveMinutes())
                .overtimeMinutes(record.getOvertimeMinutes())
                .workDurationMinutes(workDurationMinutes)
                .build();
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
        res.setLateMinutes(entity.getLateMinutes());
        res.setEarlyLeaveMinutes(entity.getEarlyLeaveMinutes());
        res.setOvertimeMinutes(entity.getOvertimeMinutes());
        return res;
    }
}

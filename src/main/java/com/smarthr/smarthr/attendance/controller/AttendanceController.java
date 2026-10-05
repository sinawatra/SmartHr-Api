/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.attendance.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smarthr.smarthr.attendance.dto.ClockInRequest;
import com.smarthr.smarthr.attendance.dto.ClockOutRequest;
import com.smarthr.smarthr.attendance.dto.AttendanceResponse;
import com.smarthr.smarthr.attendance.dto.AttendanceScoreResponse;
import com.smarthr.smarthr.common.dto.ApiResponse;
import com.smarthr.smarthr.common.dto.PagedResponse;
import com.smarthr.smarthr.attendance.service.AttendanceScoreService;
import com.smarthr.smarthr.attendance.service.AttendanceService;

/**
 *
 * @author sinawatrarith
 */
@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {
private final AttendanceService attendanceService;
    private final AttendanceScoreService attendanceScoreService;

    public AttendanceController(AttendanceService attendanceService, AttendanceScoreService attendanceScoreService) {
        this.attendanceService = attendanceService;
        this.attendanceScoreService = attendanceScoreService;
    }

    @PostMapping("/clock-in")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> clockIn(@RequestBody ClockInRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Clocked in successfully", attendanceService.clockIn(request)));
    }

    @PostMapping("/clock-out")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> clockOut(@RequestBody ClockOutRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Clocked out successfully", attendanceService.clockOut(request)));
    }

   @GetMapping("/history/{employeeId}")
    public ResponseEntity<ApiResponse<PagedResponse<AttendanceResponse>>> getAttendanceHistory(
            @PathVariable Long employeeId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "clockIn") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // Default to current month if dates aren't provided by the client
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.withDayOfMonth(1);
        }

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<AttendanceResponse> history = attendanceService.getAttendanceHistory(employeeId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Attendance history retrieved successfully", history));
    }

    /**
     * Attendance score of the logged-in employee. Defaults to the current month so far.
     */
    @GetMapping("/score/me")
    public ResponseEntity<ApiResponse<AttendanceScoreResponse>> getMyScore(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.withDayOfMonth(1);
        return ResponseEntity.ok(ApiResponse.success("My attendance score retrieved successfully",
                attendanceScoreService.getMyScore(start, end)));
    }

    /**
     * Attendance score of one employee (ADMIN, HR, their line manager, or themselves).
     * Defaults to the current month so far.
     */
    @GetMapping("/score/{employeeId}")
    public ResponseEntity<ApiResponse<AttendanceScoreResponse>> getScore(
            @PathVariable Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.withDayOfMonth(1);
        return ResponseEntity.ok(ApiResponse.success("Attendance score retrieved successfully",
                attendanceScoreService.getScore(employeeId, start, end)));
    }

    /**
     * Attendance scores of all active employees for a month (format yyyy-MM, default current month),
     * highest score first.
     */
    @GetMapping("/score")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<PagedResponse<AttendanceScoreResponse>>> getScores(
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        YearMonth yearMonth;
        try {
            yearMonth = month != null ? YearMonth.parse(month) : YearMonth.now();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid month '" + month + "', expected format yyyy-MM");
        }
        return ResponseEntity.ok(ApiResponse.success("Attendance scores retrieved successfully",
                attendanceScoreService.getScores(yearMonth, PageRequest.of(page, size))));
    }
}

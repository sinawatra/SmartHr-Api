/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smarthr.smarthr.request.ClockInRequest;
import com.smarthr.smarthr.request.ClockOutRequest;
import com.smarthr.smarthr.response.AttendanceResponse;
import com.smarthr.smarthr.service.AttendanceService;

/**
 *
 * @author sinawatrarith
 */
@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {
private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/clock-in")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<AttendanceResponse> clockIn(@RequestBody ClockInRequest request) {
        return ResponseEntity.ok(attendanceService.clockIn(request));
    }

    @PostMapping("/clock-out")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<AttendanceResponse> clockOut(@RequestBody ClockOutRequest request) {
        return ResponseEntity.ok(attendanceService.clockOut(request));
    }
}

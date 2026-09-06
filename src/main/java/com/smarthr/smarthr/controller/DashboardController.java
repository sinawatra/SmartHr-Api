package com.smarthr.smarthr.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smarthr.smarthr.response.ApiResponse;
import com.smarthr.smarthr.response.DashboardResponse;
import com.smarthr.smarthr.service.DashboardService;

import lombok.RequiredArgsConstructor;

/**
 * Exposes aggregated statistics for the HR dashboard.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        DashboardResponse response = dashboardService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved successfully", response));
    }
}

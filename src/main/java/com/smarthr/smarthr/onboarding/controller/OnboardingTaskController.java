package com.smarthr.smarthr.onboarding.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smarthr.smarthr.common.dto.ApiResponse;
import com.smarthr.smarthr.onboarding.dto.OnboardingTaskResponse;
import com.smarthr.smarthr.onboarding.service.OnboardingTaskService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing employee onboarding tasks.
 */
@RestController
@RequestMapping({"/api/v1/onboarding-tasks", "/api/v1/onboarding-tasks/", "/api/onboarding-tasks"})
@RequiredArgsConstructor
public class OnboardingTaskController {

    private final OnboardingTaskService onboardingTaskService;

    /**
     * Get all onboarding tasks for a specific employee.
     */
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<OnboardingTaskResponse>>> getTasksByEmployeeId(@PathVariable Long employeeId) {
        List<OnboardingTaskResponse> tasks = onboardingTaskService.getTasksByEmployeeId(employeeId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /**
     * Complete or un-complete ("tick") an employee's onboarding task by task ID.
     * Example: PATCH /api/v1/onboarding-tasks/5/complete?completed=true
     */
    @PatchMapping("/{taskId}/complete")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<OnboardingTaskResponse>> updateTaskCompletion(
            @PathVariable Integer taskId,
            @RequestParam(defaultValue = "true") boolean completed) {
        OnboardingTaskResponse response = onboardingTaskService.updateTaskCompletion(taskId, completed);
        String message = completed ? "Task marked as completed" : "Task marked as incomplete";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }
}

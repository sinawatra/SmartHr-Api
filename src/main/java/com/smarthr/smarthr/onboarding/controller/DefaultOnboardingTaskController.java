package com.smarthr.smarthr.onboarding.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smarthr.smarthr.onboarding.dto.DefaultOnboardingTaskRequest;
import com.smarthr.smarthr.common.dto.ApiResponse;
import com.smarthr.smarthr.onboarding.dto.DefaultOnboardingTaskResponse;
import com.smarthr.smarthr.onboarding.service.DefaultOnboardingTaskService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing default onboarding task templates.
 */
@RestController
@RequestMapping({"/api/v1/default-onboarding-tasks", "/api/v1/default-onboarding-tasks/"})
@RequiredArgsConstructor
public class DefaultOnboardingTaskController {

    private final DefaultOnboardingTaskService service;

    /**
     * Get all default onboarding tasks (optionally filtered by active status).
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<DefaultOnboardingTaskResponse>>> getAllTasks(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<DefaultOnboardingTaskResponse> tasks = activeOnly ? service.getActiveTasks() : service.getAllTasks();
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /**
     * Get default onboarding task by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<DefaultOnboardingTaskResponse>> getTaskById(@PathVariable Integer id) {
        DefaultOnboardingTaskResponse response = service.getTaskById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Create a new default onboarding task template.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<DefaultOnboardingTaskResponse>> createTask(@RequestBody DefaultOnboardingTaskRequest request) {
        DefaultOnboardingTaskResponse response = service.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Default onboarding task created successfully", response));
    }

    /**
     * Update default onboarding task template.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<DefaultOnboardingTaskResponse>> updateTask(
            @PathVariable Integer id,
            @RequestBody DefaultOnboardingTaskRequest request) {
        DefaultOnboardingTaskResponse response = service.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.success("Default onboarding task updated successfully", response));
    }

    /**
     * Delete default onboarding task template.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Integer id) {
        service.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success("Default onboarding task deleted successfully", null));
    }
}

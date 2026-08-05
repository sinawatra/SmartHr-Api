package com.smarthr.smarthr.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.smarthr.smarthr.entity.OnboardingTaskEntity;
import com.smarthr.smarthr.exception.ResourceNotFoundException;
import com.smarthr.smarthr.repository.OnboardingTaskRepository;
import com.smarthr.smarthr.response.OnboardingTaskResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnboardingTaskService {

    private final OnboardingTaskRepository onboardingTaskRepository;

    /**
     * Get all onboarding tasks assigned to a specific employee.
     */
    public List<OnboardingTaskResponse> getTasksByEmployeeId(Long employeeId) {
        return onboardingTaskRepository.findByEmployeeId(employeeId).stream()
                .map(OnboardingTaskResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Complete or update completion status ("tick") of an employee's onboarding task.
     */
    public OnboardingTaskResponse updateTaskCompletion(Integer taskId, boolean completed) {
        OnboardingTaskEntity task = onboardingTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding task not found with id: " + taskId));

        task.setCompleted(completed);
        task.setCompletedAt(completed ? LocalDateTime.now() : null);

        OnboardingTaskEntity updated = onboardingTaskRepository.save(task);
        return OnboardingTaskResponse.fromEntity(updated);
    }
}

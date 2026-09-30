package com.smarthr.smarthr.onboarding.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.employee.entity.EmployeeDetails;
import com.smarthr.smarthr.onboarding.entity.OnboardingTaskEntity;
import com.smarthr.smarthr.employee.entity.EmployementStatus;
import com.smarthr.smarthr.common.exception.ResourceNotFoundException;
import com.smarthr.smarthr.employee.repository.EmployeeRepository;
import com.smarthr.smarthr.onboarding.repository.OnboardingTaskRepository;
import com.smarthr.smarthr.onboarding.dto.OnboardingTaskResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingTaskService {

    private final OnboardingTaskRepository onboardingTaskRepository;
    private final EmployeeRepository employeeRepository;

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
     * Auto changes employee status to FullStaff if all onboarding tasks are completed.
     */
    public OnboardingTaskResponse updateTaskCompletion(Integer taskId, boolean completed) {
        OnboardingTaskEntity task = onboardingTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding task not found with id: " + taskId));

        task.setCompleted(completed);
        task.setCompletedAt(completed ? LocalDateTime.now() : null);

        OnboardingTaskEntity updated = onboardingTaskRepository.save(task);

        if (task.getEmployee() != null && task.getEmployee().getId() != null) {
            Long employeeId = task.getEmployee().getId();
            List<OnboardingTaskEntity> allTasks = onboardingTaskRepository.findByEmployeeId(employeeId);
            boolean allCompleted = !allTasks.isEmpty() && allTasks.stream().allMatch(OnboardingTaskEntity::isCompleted);

            if (allCompleted) {
                EmployeeDetails employee = employeeRepository.findById(employeeId).orElse(task.getEmployee());
                if (employee.getEmployeeStatus() == null || employee.getEmployeeStatus() == EmployementStatus.Probation) {
                    employee.setEmployeeStatus(EmployementStatus.FullStaff);
                    employeeRepository.save(employee);
                }
            }
        }

        return OnboardingTaskResponse.fromEntity(updated);
    }
}

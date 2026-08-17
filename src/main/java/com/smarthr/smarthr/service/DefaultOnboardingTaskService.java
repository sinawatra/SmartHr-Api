package com.smarthr.smarthr.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.smarthr.smarthr.entity.DefaultOnboardingTaskEntity;
import com.smarthr.smarthr.exception.ResourceNotFoundException;
import com.smarthr.smarthr.repository.DefaultOnboardingTaskRepository;
import com.smarthr.smarthr.request.DefaultOnboardingTaskRequest;
import com.smarthr.smarthr.response.DefaultOnboardingTaskResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultOnboardingTaskService {

    private final DefaultOnboardingTaskRepository repository;

    public List<DefaultOnboardingTaskResponse> getAllTasks() {
        return repository.findAll().stream()
                .map(DefaultOnboardingTaskResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<DefaultOnboardingTaskResponse> getActiveTasks() {
        return repository.findByActiveTrue().stream()
                .map(DefaultOnboardingTaskResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public DefaultOnboardingTaskResponse getTaskById(Integer id) {
        DefaultOnboardingTaskEntity task = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Default onboarding task not found with id: " + id));
        return DefaultOnboardingTaskResponse.fromEntity(task);
    }

    public DefaultOnboardingTaskResponse createTask(DefaultOnboardingTaskRequest request) {
        if (request.getTaskName() == null || request.getTaskName().isBlank()) {
            throw new IllegalArgumentException("Task name is required");
        }

        DefaultOnboardingTaskEntity task = DefaultOnboardingTaskEntity.builder()
                .taskName(request.getTaskName())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        DefaultOnboardingTaskEntity saved = repository.save(task);
        return DefaultOnboardingTaskResponse.fromEntity(saved);
    }

    public DefaultOnboardingTaskResponse updateTask(Integer id, DefaultOnboardingTaskRequest request) {
        DefaultOnboardingTaskEntity task = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Default onboarding task not found with id: " + id));

        if (request.getTaskName() != null && !request.getTaskName().isBlank()) {
            task.setTaskName(request.getTaskName());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getActive() != null) {
            task.setActive(request.getActive());
        }

        DefaultOnboardingTaskEntity updated = repository.save(task);
        return DefaultOnboardingTaskResponse.fromEntity(updated);
    }

    public void deleteTask(Integer id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Default onboarding task not found with id: " + id);
        }
        repository.deleteById(id);
    }
}

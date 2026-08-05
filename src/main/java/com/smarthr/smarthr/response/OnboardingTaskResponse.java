package com.smarthr.smarthr.response;

import java.time.LocalDateTime;

import com.smarthr.smarthr.entity.OnboardingTaskEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingTaskResponse {
    private Integer id;
    private Long employeeId;
    private String taskName;
    private boolean completed;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OnboardingTaskResponse fromEntity(OnboardingTaskEntity entity) {
        if (entity == null) {
            return null;
        }
        return OnboardingTaskResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee() != null ? entity.getEmployee().getId() : null)
                .taskName(entity.getTaskName())
                .completed(entity.isCompleted())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

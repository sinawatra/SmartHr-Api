package com.smarthr.smarthr.response;

import com.smarthr.smarthr.entity.DefaultOnboardingTaskEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefaultOnboardingTaskResponse {
    private Integer id;
    private String taskName;
    private String description;
    private boolean active;

    public static DefaultOnboardingTaskResponse fromEntity(DefaultOnboardingTaskEntity entity) {
        if (entity == null) {
            return null;
        }
        return DefaultOnboardingTaskResponse.builder()
                .id(entity.getId())
                .taskName(entity.getTaskName())
                .description(entity.getDescription())
                .active(entity.isActive())
                .build();
    }
}

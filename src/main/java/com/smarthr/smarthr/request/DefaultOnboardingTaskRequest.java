package com.smarthr.smarthr.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefaultOnboardingTaskRequest {
    private String taskName;
    private String description;
    private Boolean active;
}

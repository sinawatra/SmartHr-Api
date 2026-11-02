package com.smarthr.smarthr.notification.dto;

import java.time.Instant;

import com.smarthr.smarthr.notification.entity.NotificationEntity;
import com.smarthr.smarthr.notification.entity.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private Long referenceId;
    private boolean hasRead;
    private Instant readAt;
    private Instant createdAt;

    public static NotificationResponse fromEntity(NotificationEntity entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .referenceId(entity.getReferenceId())
                .hasRead(entity.isRead())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

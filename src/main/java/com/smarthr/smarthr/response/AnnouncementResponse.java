package com.smarthr.smarthr.response;

import java.time.Instant;

import com.smarthr.smarthr.entity.AnnouncementEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementResponse {
    private Long id;
    private String title;
    private String description;
    private Long createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;

    public static AnnouncementResponse fromEntity(AnnouncementEntity entity) {
        if (entity == null) {
            return null;
        }

        String creatorName = null;
        if (entity.getCreatedBy() != null) {
            String first = entity.getCreatedBy().getFirstName() != null ? entity.getCreatedBy().getFirstName() : "";
            String last = entity.getCreatedBy().getLastName() != null ? entity.getCreatedBy().getLastName() : "";
            creatorName = (first + " " + last).trim();
            if (creatorName.isBlank()) {
                creatorName = entity.getCreatedBy().getUsername();
            }
        }

        return AnnouncementResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdByName(creatorName)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

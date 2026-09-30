package com.smarthr.smarthr.announcement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementRequest {
    private String title;
    private String description;
    private Long companyId;
    private Long departmentId;
}

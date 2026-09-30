package com.smarthr.smarthr.department.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter 
@Setter
public class DepartmentResponse {
    private Long id;
    private String name;
    private String description;
    private Long companyId;
    private String companyName;
    private Instant createdAt;
    private Instant updatedAt;
}

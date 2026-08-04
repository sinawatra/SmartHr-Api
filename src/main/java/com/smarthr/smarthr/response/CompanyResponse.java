package com.smarthr.smarthr.response;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyResponse {
    private Long id;
    private String name;
    private String code;
    private String address;
    private String phone;
    private String email;
    private List<DepartmentResponse> departments;
    private Instant createdAt;
    private Instant updatedAt;
}

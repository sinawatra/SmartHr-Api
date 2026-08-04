package com.smarthr.smarthr.response;

import java.time.Instant;
import java.time.LocalDate;

import com.smarthr.smarthr.entity.EmployeeDetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponse {
    private Long id;
    private String username;
    private Integer roleId;
    private Integer companyId;
    private Integer departmentId;
    private Integer managerId;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String employeeStatus;
    private LocalDate hiredate;
    private LocalDate probationEndDate;
    private String profileImage;
    private LocalDate endDate;
    private Instant createdAt;
    private Instant updatedAt;

    public static EmployeeResponse fromEntity(EmployeeDetails employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .username(employee.getUsername())
                .roleId(employee.getRoleId())
                .companyId(employee.getCompanyId())
                .departmentId(employee.getDepartmentId())
                .managerId(employee.getManagerId())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phoneNumber(employee.getPhoneNumber())
                .employeeStatus(employee.getEmployeeStatus())
                .hiredate(employee.getHiredate())
                .probationEndDate(employee.getProbationEndDate())
                .profileImage(employee.getProfileImage())
                .endDate(employee.getEndDate())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }
}

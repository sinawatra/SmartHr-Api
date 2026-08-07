package com.smarthr.smarthr.request;

import java.time.LocalDate;
import java.util.List;

import com.smarthr.smarthr.enumeration.EmployementStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEmployeeRequest {
    private String username;
    private String password;
    private Integer roleId;
    private Integer companyId;
    private Integer departmentId;
    private Integer managerId;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private EmployementStatus employeeStatus;
    private LocalDate hiredate;
    private LocalDate probationEndDate;
    private String profileImage;
    private LocalDate endDate;
    private List<String> onboardingTasks;
}

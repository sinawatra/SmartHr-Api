package com.smarthr.smarthr.employee.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.smarthr.smarthr.employee.entity.EmployeeDetails;
import com.smarthr.smarthr.employee.entity.EmployementStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.smarthr.smarthr.onboarding.dto.OnboardingTaskResponse;

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
    private boolean  hasClockIn;
    private boolean hasClockOut;
    private EmployementStatus employeeStatus;
    private LocalDate hiredate;
    private LocalDate probationEndDate;
    private String profileImage;
    private String telegramUsername;
    private String telegramChatId;
    private LocalDate endDate;
    private List<OnboardingTaskResponse> onboardingTasks;
    private Instant createdAt;
    private Instant updatedAt;

    public static EmployeeResponse fromEntity(EmployeeDetails employee) {
        return fromEntity(employee, null);
    }

    public static EmployeeResponse fromEntity(EmployeeDetails employee, List<OnboardingTaskResponse> onboardingTasks) {
        return fromEntity(employee, onboardingTasks, false, false);
    }

    public static EmployeeResponse fromEntity(EmployeeDetails employee, List<OnboardingTaskResponse> onboardingTasks, boolean hasClockIn, boolean hasClockOut) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .username(employee.getUsername())
                .roleId(employee.getRole() != null ? employee.getRole().getId() : null)
                .companyId(employee.getCompanyId())
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId().intValue(): null)
                .managerId(employee.getManagerId())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phoneNumber(employee.getPhoneNumber())
                .hasClockIn(hasClockIn)
                .hasClockOut(hasClockOut)
                .employeeStatus(employee.getEmployeeStatus())
                .hiredate(employee.getHiredate())
                .probationEndDate(employee.getProbationEndDate())
                .profileImage(employee.getProfileImage())
                .telegramUsername(employee.getTelegramUsername())
                .telegramChatId(employee.getTelegramChatId())
                .endDate(employee.getEndDate())
                .onboardingTasks(onboardingTasks)
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }
}

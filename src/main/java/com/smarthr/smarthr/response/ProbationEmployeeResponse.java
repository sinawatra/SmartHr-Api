package com.smarthr.smarthr.response;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.smarthr.smarthr.entity.EmployeeDetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProbationEmployeeResponse {
    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String profileImage;
    /** Probation starts on the hire date. */
    private LocalDate startDate;
    private LocalDate probationEndDate;
    /** Days until probation ends; negative when overdue, null when no end date is set. */
    private Long remainingDays;
    /** Null when the employee has no manager or the manager no longer exists. */
    private Manager manager;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Manager {
        private Long id;
        private String employeeCode;
        private String firstName;
        private String lastName;

        public static Manager fromEntity(EmployeeDetails manager) {
            if (manager == null) {
                return null;
            }
            return Manager.builder()
                    .id(manager.getId())
                    .employeeCode(manager.getEmployeeCode())
                    .firstName(manager.getFirstName())
                    .lastName(manager.getLastName())
                    .build();
        }
    }

    public static ProbationEmployeeResponse fromEntity(EmployeeDetails employee, EmployeeDetails manager, LocalDate today) {
        LocalDate endDate = employee.getProbationEndDate();
        return ProbationEmployeeResponse.builder()
                .manager(Manager.fromEntity(manager))
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .profileImage(employee.getProfileImage())
                .startDate(employee.getHiredate())
                .probationEndDate(endDate)
                .remainingDays(endDate != null ? ChronoUnit.DAYS.between(today, endDate) : null)
                .build();
    }
}

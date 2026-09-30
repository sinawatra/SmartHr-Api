package com.smarthr.smarthr.employee.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 *
 * @author sinawatrarith
 */
public enum EmployementStatus {
    FullStaff,
    Probation,
    Contract,
    Intern,
    Resigned,
    Terminated;

    @JsonCreator
    public static EmployementStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (EmployementStatus status : EmployementStatus.values()) {
            if (status.name().equalsIgnoreCase(trimmed)) {
                return status;
            }
        }
        return EmployementStatus.FullStaff;
    }
}

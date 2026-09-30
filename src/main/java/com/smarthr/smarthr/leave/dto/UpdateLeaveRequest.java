package com.smarthr.smarthr.leave.dto;

import com.smarthr.smarthr.leave.entity.LeaveStatus;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter 
@Setter
public class UpdateLeaveRequest {
    @NotNull(message = "Status is required")
    private LeaveStatus status;
    private String reason; // optional: e.g., rejection reason
}

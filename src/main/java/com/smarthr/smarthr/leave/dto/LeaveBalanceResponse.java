package com.smarthr.smarthr.leave.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an employee's leave balance for a single leave type.
 *
 * <p>{@code remainingDays} reserves pending requests: it is
 * {@code entitledDays - approvedDays - pendingDays}, so a request that has
 * not yet been approved by the LINE_MANAGER still holds the balance.
 *
 * @author sinawatrarith
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceResponse {
    private Integer leaveTypeId;
    private String leaveTypeName;
    private int entitledDays;
    private long approvedDays;
    private long pendingDays;
    private long remainingDays;
}

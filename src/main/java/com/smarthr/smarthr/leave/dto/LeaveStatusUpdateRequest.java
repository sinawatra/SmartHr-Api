/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.leave.dto;

import com.smarthr.smarthr.leave.entity.LeaveStatus;

import jakarta.validation.constraints.NotNull;

/**
 *
 * @author sinawatrarith
 */
public record LeaveStatusUpdateRequest(
    @NotNull LeaveStatus status, // Enum: APPROVED, REJECTED
    String rejectionReason       // Optional feedback
) {}
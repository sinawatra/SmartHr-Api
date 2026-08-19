/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.response;

import com.smarthr.smarthr.enumeration.LeaveStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author sinawatrarith
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter 
@Setter
public class UpdateLeaveStatusResponse {
    private LeaveStatus status;
    private String reason; // optional: e.g., rejection reason
    private String approvedBy;


}

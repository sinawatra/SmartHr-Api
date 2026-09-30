/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.leave.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author sinawatrarith
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTypesRequest {
    private String leaveTypeName;
    private Integer maxDays;
    private String leaveTypeDescription;

}

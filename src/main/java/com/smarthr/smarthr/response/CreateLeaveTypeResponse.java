/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author sinawatrarith
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeaveTypeResponse {
    private String leaveTypeName;
    private Integer maxDays;
    private String leaveTypeDescription;

}

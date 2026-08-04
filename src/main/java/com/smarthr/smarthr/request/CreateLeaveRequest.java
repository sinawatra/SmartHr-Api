/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sinawatrarith
 */
@Getter 
@Setter
public class CreateLeaveRequest {
    private Integer leaveTypeId; 
    // 2. Using LocalDate automatically handles ISO "YYYY-MM-DD" formatting
    private LocalDate startDate; 
    private LocalDate endDate;
    private String reason;
}

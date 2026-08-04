/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.response;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sinawatrarith
 */
@Getter
@Setter
public class AttendanceResponse {
private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private Long workDurationMinutes;
    private String status;
    private String notes;
}

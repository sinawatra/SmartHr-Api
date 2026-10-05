package com.smarthr.smarthr.company.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRequest {
    private String name;
    private String code;
    private String address;
    private String phone;
    private String email;
    private String telegramChatId;
    private String latitute;
    private String longitude;
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private Integer lateGraceMinutes;
    private List<DayOfWeek> workingDays;
}

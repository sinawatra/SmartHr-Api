package com.smarthr.smarthr.request;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClockOutRequest {
    private Long employeeId;
    private String notes;
    private MultipartFile imageFile;
}

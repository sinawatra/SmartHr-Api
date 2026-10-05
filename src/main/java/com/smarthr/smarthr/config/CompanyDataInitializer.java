package com.smarthr.smarthr.config;

import java.time.LocalTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.smarthr.smarthr.company.entity.CompanyEntity;
import com.smarthr.smarthr.company.repository.CompanyRepository;

import lombok.RequiredArgsConstructor;

/**
 * Seeds one dummy company (with a work schedule) when no company exists yet.
 */
@Component
@RequiredArgsConstructor
public class CompanyDataInitializer implements CommandLineRunner {

    private final CompanyRepository companyRepository;

    @Override
    public void run(String... args) {
        if (companyRepository.count() == 0) {
            CompanyEntity company = CompanyEntity.builder()
                    .name("SmartHR Demo Co., Ltd.")
                    .code("DEMO")
                    .address("No. 123, Street 271, Phnom Penh, Cambodia")
                    .phone("+855 12 345 678")
                    .email("contact@smarthr-demo.com")
                    .latitude("11.5564")
                    .longitude("104.9282")
                    .workStartTime(LocalTime.of(8, 0))
                    .workEndTime(LocalTime.of(17, 0))
                    .lateGraceMinutes(10)
                    .workingDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                    .build();

            companyRepository.save(company);
        }
    }
}

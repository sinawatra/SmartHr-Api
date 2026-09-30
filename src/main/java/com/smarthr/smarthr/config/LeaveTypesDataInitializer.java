package com.smarthr.smarthr.config;


import com.smarthr.smarthr.leave.entity.LeaveTypeEntity;
import com.smarthr.smarthr.leave.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LeaveTypesDataInitializer  implements CommandLineRunner {

    private final LeaveTypeRepository leaveTypeRepository;

    @Override
    public void run(String... args) {
        if (leaveTypeRepository.count() == 0) {

            List<LeaveTypeEntity> defaultLeaveTypes = List.of(
                    LeaveTypeEntity.builder()
                            .name("Annual Leave")
                            .maxDays(18)
                            .description("Annual paid leave for employees.")
                            .build(),
                    LeaveTypeEntity.builder()
                            .name("Birthday Leave")
                            .maxDays(1)
                            .description("Birthday Leave.")
                            .build(),

                    LeaveTypeEntity.builder()
                            .name("Sick Leave")
                            .maxDays(7)
                            .description("Leave taken when an employee is sick.")
                            .build(),

                    LeaveTypeEntity.builder()
                            .name("Unpaid Leave")
                            .maxDays(30)
                            .description("Leave without salary payment.")
                            .build()
            );

            leaveTypeRepository.saveAll(defaultLeaveTypes);
        }
    }


}

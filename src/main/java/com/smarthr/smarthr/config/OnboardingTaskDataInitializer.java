package com.smarthr.smarthr.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.smarthr.smarthr.onboarding.entity.DefaultOnboardingTaskEntity;
import com.smarthr.smarthr.onboarding.repository.DefaultOnboardingTaskRepository;

import lombok.RequiredArgsConstructor;

/**
 * Initializes default sample onboarding tasks in the database if empty.
 */
@Component
@RequiredArgsConstructor
public class OnboardingTaskDataInitializer implements CommandLineRunner {

    private final DefaultOnboardingTaskRepository defaultOnboardingTaskRepository;

    @Override
    public void run(String... args) {
        if (defaultOnboardingTaskRepository.count() == 0) {
            List<DefaultOnboardingTaskEntity> defaultTasks = List.of(
                DefaultOnboardingTaskEntity.builder()
                    .taskName("Birth Certificate Submission")
                    .description("Submit official copy of Birth Certificate for employee profile verification.")
                    .active(true)
                    .build(),
                DefaultOnboardingTaskEntity.builder()
                    .taskName("Employment Certificate")
                    .description("Provide previous employment certificate or reference letter.")
                    .active(true)
                    .build(),
                DefaultOnboardingTaskEntity.builder()
                    .taskName("National ID / Passport Copy")
                    .description("Submit government-issued photo ID or passport.")
                    .active(true)
                    .build(),
                DefaultOnboardingTaskEntity.builder()
                    .taskName("Direct Deposit & Bank Details")
                    .description("Submit bank account details for salary payroll direct deposit.")
                    .active(true)
                    .build(),
                DefaultOnboardingTaskEntity.builder()
                    .taskName("Signed NDA & Code of Conduct")
                    .description("Review and sign Company Non-Disclosure Agreement & Employee Code of Conduct.")
                    .active(true)
                    .build(),
                DefaultOnboardingTaskEntity.builder()
                    .taskName("IT Workstation & Credentials Setup")
                    .description("Complete laptop setup, company email, and system access configuration.")
                    .active(true)
                    .build()
            );

            defaultOnboardingTaskRepository.saveAll(defaultTasks);
        }
    }
}

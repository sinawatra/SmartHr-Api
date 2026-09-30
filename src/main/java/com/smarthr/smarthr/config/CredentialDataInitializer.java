package com.smarthr.smarthr.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.smarthr.smarthr.employee.entity.EmployeeDetails;
import com.smarthr.smarthr.role.entity.RoleEntity;
import com.smarthr.smarthr.employee.entity.EmployementStatus;
import com.smarthr.smarthr.employee.repository.EmployeeRepository;
import com.smarthr.smarthr.role.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Seeds a single admin credential into the database if it does not already exist.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class CredentialDataInitializer implements CommandLineRunner {

    private static final String SEED_USERNAME = "Sinawatra Rith";
    private static final String SEED_PASSWORD = "$Rith$9990";
    private static final String SEED_ROLE = "ADMIN";

    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (employeeRepository.existsByUsername(SEED_USERNAME)) {
            return;
        }

        RoleEntity role = roleRepository.findByName(SEED_ROLE)
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name(SEED_ROLE).build()));

        EmployeeDetails employee = EmployeeDetails.builder()
                .username(SEED_USERNAME)
                .password(passwordEncoder.encode(SEED_PASSWORD))
                .role(role)
                .firstName("Sinawatra")
                .lastName("Rith")
                .email("watrasinarith@gmail.com")
                .employeeStatus(EmployementStatus.FullStaff)
                .build();

        employeeRepository.save(employee);
    }
}

package com.smarthr.smarthr.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.smarthr.smarthr.role.entity.RoleEntity;
import com.smarthr.smarthr.role.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Seeds the base set of roles (USER, ADMIN) into the database if missing.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class RoleDataInitializer implements CommandLineRunner {

    private static final List<String> DEFAULT_ROLES = List.of("USER", "ADMIN", "LINE_MANAGER");

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        for (String roleName : DEFAULT_ROLES) {
            if (!roleRepository.existsByName(roleName)) {
                roleRepository.save(RoleEntity.builder().name(roleName).build());
            }
        }
    }
}

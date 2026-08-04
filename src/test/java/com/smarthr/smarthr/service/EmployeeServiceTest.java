package com.smarthr.smarthr.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.smarthr.smarthr.entity.EmployeeDetails;
import com.smarthr.smarthr.entity.RoleEntity;
import com.smarthr.smarthr.exception.InvalidCredentialsException;
import com.smarthr.smarthr.exception.UserAlreadyExistsException;
import com.smarthr.smarthr.repository.EmployeeRepository;
import com.smarthr.smarthr.repository.RoleRepository;
import com.smarthr.smarthr.request.CreateEmployeeRequest;
import com.smarthr.smarthr.request.LoginRequest;
import com.smarthr.smarthr.response.EmployeeResponse;
import com.smarthr.smarthr.response.LoginResponse;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private com.smarthr.smarthr.security.JwtTokenProvider tokenProvider;

    @InjectMocks
    private EmployeeService employeeService;

    private RoleEntity adminRoleEntity;
    private RoleEntity userRoleEntity;
    private EmployeeDetails mockAdmin;
    private EmployeeDetails mockUser;

    @BeforeEach
    void setUp() {
        adminRoleEntity = RoleEntity.builder().id(1).name("ADMIN").build();
        userRoleEntity = RoleEntity.builder().id(2).name("USER").build();

        mockAdmin = EmployeeDetails.builder()
                .id(1L)
                .username("admin")
                .password("encoded_admin_pass")
                .role(adminRoleEntity)
                .firstName("Admin")
                .lastName("User")
                .email("admin@smarthr.com")
                .build();

        mockUser = EmployeeDetails.builder()
                .id(2L)
                .username("john_doe")
                .password("encoded_user_pass")
                .role(userRoleEntity)
                .firstName("John")
                .lastName("Doe")
                .email("john@smarthr.com")
                .build();
    }

    @Test
    void testLoginSuccess_Admin() {
        LoginRequest request = new LoginRequest("admin", "admin123");

        when(employeeRepository.findByUsername("admin")).thenReturn(Optional.of(mockAdmin));
        when(passwordEncoder.matches("admin123", "encoded_admin_pass")).thenReturn(true);
        when(tokenProvider.generateToken("admin", "ADMIN")).thenReturn("mock_token_admin");

        LoginResponse response = employeeService.login(request);

        assertNotNull(response);
        assertEquals("admin", response.getUsername());
        assertEquals("mock_token_admin", response.getToken());
    }

    @Test
    void testLoginSuccess_User() {
        LoginRequest request = new LoginRequest("john_doe", "user123");

        when(employeeRepository.findByUsername("john_doe")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("user123", "encoded_user_pass")).thenReturn(true);
        when(tokenProvider.generateToken("john_doe", "USER")).thenReturn("mock_token_user");

        LoginResponse response = employeeService.login(request);

        assertNotNull(response);
        assertEquals("john_doe", response.getUsername());
        assertEquals("mock_token_user", response.getToken());
    }

    @Test
    void testLoginInvalidPassword() {
        LoginRequest request = new LoginRequest("john_doe", "wrongpass");

        when(employeeRepository.findByUsername("john_doe")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongpass", "encoded_user_pass")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> employeeService.login(request));
    }

    @Test
    void testLoginUserNotFound() {
        LoginRequest request = new LoginRequest("unknown", "pass");

        when(employeeRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> employeeService.login(request));
    }

    @Test
    void testAdminCreateEmployeeSuccess() {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .username("new_employee")
                .password("emp123")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@smarthr.com")
                .build();

        EmployeeDetails savedEmployee = EmployeeDetails.builder()
                .id(3L)
                .username("new_employee")
                .password("encoded_emp123")
                .role(userRoleEntity)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@smarthr.com")
                .build();

        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRoleEntity));
        when(employeeRepository.existsByUsername("new_employee")).thenReturn(false);
        when(employeeRepository.existsByEmail("jane@smarthr.com")).thenReturn(false);
        when(passwordEncoder.encode("emp123")).thenReturn("encoded_emp123");
        when(employeeRepository.save(any(EmployeeDetails.class))).thenReturn(savedEmployee);

        EmployeeResponse response = employeeService.createEmployee(request);

        assertNotNull(response);
        assertEquals("new_employee", response.getUsername());
        assertEquals("Jane", response.getFirstName());
    }

    @Test
    void testAdminCreateEmployeeUsernameExists() {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .username("john_doe")
                .password("emp123")
                .build();

        when(employeeRepository.existsByUsername("john_doe")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> employeeService.createEmployee(request));
    }
}

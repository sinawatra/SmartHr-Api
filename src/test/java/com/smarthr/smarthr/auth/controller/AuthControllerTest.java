package com.smarthr.smarthr.auth.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.smarthr.smarthr.employee.dto.CreateEmployeeRequest;
import com.smarthr.smarthr.auth.dto.LoginRequest;
import com.smarthr.smarthr.common.dto.ApiResponse;
import com.smarthr.smarthr.employee.dto.EmployeeResponse;
import com.smarthr.smarthr.auth.dto.LoginResponse;
import com.smarthr.smarthr.employee.service.EmployeeService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private AuthController authController;

    private LoginResponse mockLoginResponse;
    private EmployeeResponse mockEmployeeResponse;

    @BeforeEach
    void setUp() {
        mockLoginResponse = LoginResponse.builder()
                .id(1L)
                .username("admin")
                .build();

        mockEmployeeResponse = EmployeeResponse.builder()
                .id(2L)
                .username("emp_user")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();
    }

    @Test
    void testLoginControllerEndpoint() {
        LoginRequest request = new LoginRequest("admin", "password123");
        when(employeeService.login(any(LoginRequest.class))).thenReturn(mockLoginResponse);

        ResponseEntity<ApiResponse<LoginResponse>> responseEntity = authController.login(request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().isSuccess());
        assertEquals("admin", responseEntity.getBody().getData().getUsername());
    }

    @Test
    void testInitAdminControllerEndpoint() {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .username("admin_user")
                .password("adminpass")
                .build();

        when(employeeService.createEmployee(any(CreateEmployeeRequest.class))).thenReturn(mockEmployeeResponse);

        ResponseEntity<ApiResponse<EmployeeResponse>> responseEntity = authController.initAdmin(request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().isSuccess());
    }

    @Test
    void testGetProfileInfoControllerEndpoint() {
        mockEmployeeResponse.setHasClockIn(true);
        mockEmployeeResponse.setHasClockOut(false);
        when(employeeService.getProfileInfo()).thenReturn(mockEmployeeResponse);

        ResponseEntity<ApiResponse<EmployeeResponse>> responseEntity = authController.getProfileInfo();

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().isSuccess());
        assertTrue(responseEntity.getBody().getData().isHasClockIn());
        assertFalse(responseEntity.getBody().getData().isHasClockOut());
    }
}

package com.smarthr.smarthr.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smarthr.smarthr.request.CreateEmployeeRequest;
import com.smarthr.smarthr.request.LoginRequest;
import com.smarthr.smarthr.request.RefreshTokenRequest;
import com.smarthr.smarthr.response.ApiResponse;
import com.smarthr.smarthr.response.EmployeeResponse;
import com.smarthr.smarthr.response.LoginResponse;
import com.smarthr.smarthr.service.EmployeeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmployeeService employeeService;

    /**
     * Endpoint for user and admin login.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = employeeService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Exchange a valid refresh token for a new access token and refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@RequestBody RefreshTokenRequest request) {
        LoginResponse response = employeeService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    /**
     * Endpoint to initialize an initial Admin account.
     */
    @PostMapping("/init-admin")
    public ResponseEntity<ApiResponse<EmployeeResponse>> initAdmin(@RequestBody CreateEmployeeRequest request) {
        request.setRoleId(1); 
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.ok(ApiResponse.success("Admin created successfully", response));
    }

    /**
     * Endpoint to get profile/account info for currently authenticated user.
     */
    @GetMapping({"/profile", "/info", "/me"})
    public ResponseEntity<ApiResponse<EmployeeResponse>> getProfileInfo() {
        EmployeeResponse response = employeeService.getProfileInfo();
        return ResponseEntity.ok(ApiResponse.success("Profile info retrieved successfully", response));
    }
}

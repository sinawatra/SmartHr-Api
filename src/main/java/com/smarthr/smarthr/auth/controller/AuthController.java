package com.smarthr.smarthr.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smarthr.smarthr.auth.dto.ChangePasswordRequest;
import com.smarthr.smarthr.employee.dto.CreateEmployeeRequest;
import com.smarthr.smarthr.auth.dto.LoginRequest;
import com.smarthr.smarthr.auth.dto.RefreshTokenRequest;
import com.smarthr.smarthr.common.dto.ApiResponse;
import com.smarthr.smarthr.employee.dto.EmployeeResponse;
import com.smarthr.smarthr.auth.dto.LoginResponse;
import com.smarthr.smarthr.employee.service.EmployeeService;

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

    /**
     * Endpoint for the currently authenticated user to change their own password.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody ChangePasswordRequest request) {
        employeeService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}

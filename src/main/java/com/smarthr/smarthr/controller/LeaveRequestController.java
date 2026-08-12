/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smarthr.smarthr.enumeration.LeaveStatus;
import com.smarthr.smarthr.request.CreateLeaveRequest;
import com.smarthr.smarthr.request.LeaveTypesRequest;
import com.smarthr.smarthr.response.ApiResponse;
import com.smarthr.smarthr.response.CreateLeaveTypeResponse;
import com.smarthr.smarthr.response.LeaveRequestResponse;
import com.smarthr.smarthr.response.PagedResponse;
import com.smarthr.smarthr.service.LeaveRequestService;


/**
 *
 * @author sinawatrarith
 */
@RestController
@RequestMapping("/api/v1/leave-requests")
public class LeaveRequestController {
    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    // 1. Any logged-in user can submit a leave request
    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<LeaveRequestResponse> createRequest(@RequestBody CreateLeaveRequest request) {
        return ResponseEntity.ok(leaveRequestService.create(request));
    }

    // 2. Regular users can ONLY view their own leave requests with pagination
    @GetMapping("/my-requests")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<PagedResponse<LeaveRequestResponse>> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(leaveRequestService.getRequestsForCurrentUser(pageable));
    }

    // 3. ONLY ADMINs can approve or reject leave requests
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'LINE_MANAGER')")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id, 
            @RequestBody LeaveStatus status) {
        
        leaveRequestService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

    // 4. Admins can view all leave requests with pagination
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PagedResponse<LeaveRequestResponse>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(leaveRequestService.getAllRequests(pageable)); }


    // 5. Get all leave statuses
    @GetMapping("/statuses")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'LINE_MANAGER')")
    public ResponseEntity<ApiResponse<List<String>>> getAllLeaveStatuses() {
        List<String> statuses = leaveRequestService.getAllLeaveStatuses();
        return ResponseEntity.ok(ApiResponse.success(statuses)); 
    }

    // 6. Create a leave type 
    @PostMapping("/leave-types")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CreateLeaveTypeResponse>> createLeaveType(@RequestBody LeaveTypesRequest leaveTypeRequest) {
        CreateLeaveTypeResponse createdLeaveType = leaveRequestService.createLeaveType(leaveTypeRequest);
        return ResponseEntity.ok(ApiResponse.success("Leave type created successfully", createdLeaveType));
    }

    // 7. Get all leave types
    @GetMapping("/leave-types")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'LINE_MANAGER')")
    public ResponseEntity<ApiResponse<List<CreateLeaveTypeResponse>>> getAllLeaveTypes() {
        List<CreateLeaveTypeResponse> leaveTypes = leaveRequestService.getAllLeaveTypes();
        return ResponseEntity.ok(ApiResponse.success(leaveTypes));  }
}

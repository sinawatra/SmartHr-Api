/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.controller;

import java.util.List;

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

import com.smarthr.smarthr.request.CreateLeaveRequest;
import com.smarthr.smarthr.response.LeaveRequestResponse;
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

    // 2. Regular users can ONLY view their own leave requests
    @GetMapping("/my-requests")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<List<LeaveRequestResponse>> getMyRequests() {
        return ResponseEntity.ok(leaveRequestService.getRequestsForCurrentUser());
    }

    // 3. ONLY ADMINs can approve or reject leave requests
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        leaveRequestService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

}

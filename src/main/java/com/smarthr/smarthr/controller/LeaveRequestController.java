/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.smarthr.smarthr.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import com.smarthr.smarthr.request.CreateLeaveRequest;
import com.smarthr.smarthr.request.LeaveTypesRequest;
import com.smarthr.smarthr.request.UpdateLeaveRequest;
import com.smarthr.smarthr.response.ApiResponse;
import com.smarthr.smarthr.response.CreateLeaveTypeResponse;
import com.smarthr.smarthr.response.LeaveBalanceResponse;
import com.smarthr.smarthr.response.LeaveRequestResponse;
import com.smarthr.smarthr.response.PagedResponse;
import com.smarthr.smarthr.response.UpdateLeaveStatusResponse;
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

    // 1a. Any logged-in user can submit a leave request via JSON
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> createRequest(@RequestBody CreateLeaveRequest request) {
        LeaveRequestResponse response = leaveRequestService.create(request, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave requested successfully", response));
    }

    // 1b. Any logged-in user can submit a leave request with file attachment (Images, PDF, etc.)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> createRequestWithFile(
            @ModelAttribute CreateLeaveRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        MultipartFile attachment = (file != null && !file.isEmpty()) ? file : request.getFile();
        LeaveRequestResponse response = leaveRequestService.create(request, attachment);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave requested successfully with attachment", response));
    }

    // 2. Regular users can ONLY view their own leave requests with pagination
    @GetMapping("/my-requests")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<PagedResponse<LeaveRequestResponse>>> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<LeaveRequestResponse> response = leaveRequestService.getRequestsForCurrentUser(pageable);

        return ResponseEntity.ok(ApiResponse.success("My leave requests retrieved successfully", response));
    }

    // 2b. Any logged-in user can view their own leave balance (entitlement minus
    //     approved and pending days) for every leave type
    @GetMapping("/balance")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'LINE_MANAGER')")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> getMyLeaveBalance() {
        List<LeaveBalanceResponse> balance = leaveRequestService.getLeaveBalanceForCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Leave balance retrieved successfully", balance));
    }

    // 2c. A line manager sees leave requests from their direct reports that are
    //     awaiting a decision; an ADMIN sees every pending request
    @GetMapping("/pending-approvals")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'LINE_MANAGER')")
    public ResponseEntity<ApiResponse<PagedResponse<LeaveRequestResponse>>> getPendingApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<LeaveRequestResponse> response = leaveRequestService.getPendingApprovalsForCurrentManager(pageable);

        return ResponseEntity.ok(ApiResponse.success("Pending leave approvals retrieved successfully", response));
    }

    // 3. A line manager (for their reports) or an ADMIN can approve or reject leave requests
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'LINE_MANAGER')")
    public ResponseEntity<ApiResponse<UpdateLeaveStatusResponse>> updateStatus(
            @PathVariable Long id, 
            @Valid @RequestBody UpdateLeaveRequest dto) {
        
        UpdateLeaveStatusResponse response = leaveRequestService.updateStatus(id, dto);

        return ResponseEntity.ok(ApiResponse.success("Update Leave Status Successfully", response));
    }

    // 4. Admins can view all leave requests with pagination
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<LeaveRequestResponse>>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<LeaveRequestResponse> response = leaveRequestService.getAllRequests(pageable);

        return ResponseEntity.ok(ApiResponse.success("All leave requests retrieved successfully", response));
    }

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

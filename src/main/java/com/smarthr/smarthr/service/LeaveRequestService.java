package com.smarthr.smarthr.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.entity.EmployeeDetails;
import com.smarthr.smarthr.entity.LeaveRequestEntity;
import com.smarthr.smarthr.entity.LeaveTypeEntity;
import com.smarthr.smarthr.enumeration.LeaveStatus;
import com.smarthr.smarthr.repository.EmployeeRepository;
import com.smarthr.smarthr.repository.LeaveRequestRepository;
import com.smarthr.smarthr.repository.LeaveTypeRepository;
import com.smarthr.smarthr.request.CreateLeaveRequest;
import com.smarthr.smarthr.response.LeaveRequestResponse;
import com.smarthr.smarthr.response.PagedResponse;

/**
 * Service class handling leave request business logic.
 *
 * @author sinawatrarith
 */
@Service
public class LeaveRequestService {
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public LeaveRequestService(EmployeeRepository employeeRepository,
                               LeaveTypeRepository leaveTypeRepository,
                               LeaveRequestRepository leaveRequestRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    /**
     * Create leave request for current authenticated user using Bearer Token
     */
    @Transactional
    public LeaveRequestResponse create(CreateLeaveRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        EmployeeDetails employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found for authenticated user: " + username));
        return createLeaveRequest(employee.getId(), request);
    }

    @Transactional
    public LeaveRequestResponse createLeaveRequest(Long employeeId, CreateLeaveRequest request) {
        EmployeeDetails employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        // 2. Fetch LeaveType using the ID submitted in DTO
        LeaveTypeEntity leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Leave Type ID: " + request.getLeaveTypeId()));

        // 3. Build & Save Leave Request
        LeaveRequestEntity leaveRequest = new LeaveRequestEntity();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus(LeaveStatus.PENDING);

        LeaveRequestEntity saved = leaveRequestRepository.save(leaveRequest);
        return mapToResponse(saved);
    }

    @Transactional
    public void updateStatus(Long id, String statusStr) {
        // 1. Fetch leave request
        LeaveRequestEntity leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found with ID: " + id));

        // 2. Parse String to Enum safely
        LeaveStatus targetStatus;
        try {
            targetStatus = LeaveStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid leave status: '" + statusStr + 
                    "'. Allowed values: APPROVED, REJECTED, PENDING");
        }

        // 3. Update status
        leaveRequest.setStatus(targetStatus);

        if (targetStatus == LeaveStatus.APPROVED) {
            leaveRequest.setApprovedAt(LocalDateTime.now());
        }

        // 4. Save entity
        leaveRequestRepository.save(leaveRequest);
    }

    @Transactional
    public void deleteLeaveRequest(Long id) {
        LeaveRequestEntity leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found with ID: " + id));
        leaveRequestRepository.delete(leaveRequest);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getRequestsForCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        EmployeeDetails employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found for authenticated user: " + username));

        return leaveRequestRepository.findByEmployeeId(employee.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeaveRequestResponse> getRequestsForCurrentUser(Pageable pageable) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        EmployeeDetails employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found for authenticated user: " + username));

        Page<LeaveRequestEntity> page = leaveRequestRepository.findByEmployeeId(employee.getId(), pageable);
        List<LeaveRequestResponse> content = page.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.of(page, content);
    }

    private LeaveRequestResponse mapToResponse(LeaveRequestEntity entity) {
        return new LeaveRequestResponse(
                entity.getId(),
                entity.getEmployee().getId(),
                entity.getLeaveType().getId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getReason(),
                entity.getStatus().name(),
                entity.getApprovedAt()
        );
    }
}


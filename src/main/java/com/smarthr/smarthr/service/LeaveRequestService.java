package com.smarthr.smarthr.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

import com.smarthr.smarthr.entity.EmployeeDetails;
import com.smarthr.smarthr.entity.LeaveRequestEntity;
import com.smarthr.smarthr.entity.LeaveTypeEntity;
import com.smarthr.smarthr.enumeration.LeaveStatus;
import com.smarthr.smarthr.repository.EmployeeRepository;
import com.smarthr.smarthr.repository.LeaveRequestRepository;
import com.smarthr.smarthr.repository.LeaveTypeRepository;
import com.smarthr.smarthr.request.CreateLeaveRequest;
import com.smarthr.smarthr.request.LeaveTypesRequest;
import com.smarthr.smarthr.request.UpdateLeaveRequest;
import com.smarthr.smarthr.response.CreateLeaveTypeResponse;
import com.smarthr.smarthr.response.LeaveRequestResponse;
import com.smarthr.smarthr.response.PagedResponse;
import com.smarthr.smarthr.response.UpdateLeaveStatusResponse;

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
    private final CloudinaryService cloudinaryService;

    public LeaveRequestService(EmployeeRepository employeeRepository,
                                LeaveTypeRepository leaveTypeRepository,
                                LeaveRequestRepository leaveRequestRepository,
                                CloudinaryService cloudinaryService) {
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Create leave request for current authenticated user using Bearer Token
     */
    @Transactional
    public LeaveRequestResponse create(CreateLeaveRequest request) {
        return create(request, null);
    }

    @Transactional
    public LeaveRequestResponse create(CreateLeaveRequest request, MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        EmployeeDetails employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found for authenticated user: " + username));
        return createLeaveRequest(employee.getId(), request, file);
    }

    @Transactional
    public LeaveRequestResponse createLeaveRequest(Long employeeId, CreateLeaveRequest request) {
        return createLeaveRequest(employeeId, request, null);
    }

    @Transactional
    public LeaveRequestResponse createLeaveRequest(Long employeeId, CreateLeaveRequest request, MultipartFile file) {
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

        // 4. Handle attachment upload (Image or Document like PDF) via Cloudinary
        if (file != null && !file.isEmpty()) {
            try {
                Map<?, ?> uploadResult = cloudinaryService.uploadFile(file, "leave_attachments");
                if (uploadResult != null && uploadResult.containsKey("secure_url")) {
                    leaveRequest.setAttachmentUrl(uploadResult.get("secure_url").toString());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload leave attachment: " + e.getMessage(), e);
            }
        }

        LeaveRequestEntity saved = leaveRequestRepository.save(leaveRequest);
        return mapToResponse(saved);
    }

    @Transactional
    public UpdateLeaveStatusResponse updateStatus(Long id, UpdateLeaveRequest dto) {
        // 1. Validate non-null inputs
        if (id == null) {
            throw new IllegalArgumentException("Leave request ID cannot be null.");
        }
        if (dto == null || dto.getStatus() == null) {
            throw new IllegalArgumentException("Leave status cannot be null.");
        }

        // 2. Fetch leave request
        LeaveRequestEntity leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found with ID: " + id));

        // 3. Prevent updating already finalized requests
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Cannot update leave request. Current status is already " 
                    + leaveRequest.getStatus());
        }

        // 4. Handle status transitions and domain logic
        if (dto.getStatus() == LeaveStatus.APPROVED) {
            leaveRequest.setApprovedAt(LocalDateTime.now());
        }

        if (dto.getReason() != null && !dto.getReason().isBlank()) {
            leaveRequest.setRemarks(dto.getReason());
        }

        //Get the approvalBy 
        leaveRequest.setStatus(dto.getStatus());

        // 5. Save entity
        LeaveRequestEntity saved = leaveRequestRepository.save(leaveRequest);

        return UpdateLeaveStatusResponse.builder()
                .status(saved.getStatus())
                .reason(saved.getRemarks() != null ? saved.getRemarks() : dto.getReason())
                .build();
    }

    @Transactional
    public UpdateLeaveStatusResponse updateStatus(Long id, LeaveStatus status) {
        return updateStatus(id, new UpdateLeaveRequest(status, null));
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
                entity.getAttachmentUrl(),
                entity.getStatus().name(),
                entity.getApprovedAt()
        );
    }

    //getAllRequests
    @Transactional(readOnly = true)
    public PagedResponse<LeaveRequestResponse> getAllRequests(Pageable pageable) {
        Page<LeaveRequestEntity> page = leaveRequestRepository.findAll(pageable);
        List<LeaveRequestResponse> content = page.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return PagedResponse.of(page, content);}


    //Get Leave Enum Status 
    @Transactional(readOnly = true)
    public List<String> getAllLeaveStatuses() {
        return List.of(LeaveStatus.values())
                .stream()
                .map(Enum::name)
                .collect(Collectors.toList());}

      
    //Create leave type 
    @Transactional
    public CreateLeaveTypeResponse createLeaveType(LeaveTypesRequest leaveTypeRequest) {
        LeaveTypeEntity leaveType = new LeaveTypeEntity();
        leaveType.setName(leaveTypeRequest.getLeaveTypeName());
        leaveType.setMaxDays(leaveTypeRequest.getMaxDays());
        leaveType.setDescription(leaveTypeRequest.getLeaveTypeDescription());
        leaveType = leaveTypeRepository.save(leaveType);
        return new CreateLeaveTypeResponse(
                leaveType.getName(),
                leaveType.getMaxDays(),
                leaveType.getDescription()
        );
    }

    //get leave type 
    @Transactional(readOnly = true)
    public List<CreateLeaveTypeResponse> getAllLeaveTypes() {
        List<LeaveTypeEntity> leaveTypes = leaveTypeRepository.findAll();
        return leaveTypes.stream()
                .map(leaveType -> new CreateLeaveTypeResponse(
                        leaveType.getName(),
                        leaveType.getMaxDays(),
                        leaveType.getDescription()
                ))
                .collect(Collectors.toList());  
            }}
        


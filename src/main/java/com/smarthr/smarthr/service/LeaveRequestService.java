package com.smarthr.smarthr.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
import com.smarthr.smarthr.response.LeaveBalanceResponse;
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

        // 2b. Enforce the leave balance. Pending requests are reserved, so the new
        //     request plus everything already approved or pending for this leave
        //     type this year must not exceed the entitlement.
        long requestedDays = countDays(request.getStartDate(), request.getEndDate());
        if (requestedDays <= 0) {
            throw new IllegalArgumentException("Leave end date must be on or after the start date.");
        }
        int entitledDays = leaveType.getMaxDays() != null ? leaveType.getMaxDays() : 0;
        long alreadyReserved = reservedDaysForType(
                employee.getId(), leaveType.getId(), request.getStartDate().getYear());
        if (requestedDays + alreadyReserved > entitledDays) {
            throw new IllegalStateException("Insufficient " + leaveType.getName() + " balance: "
                    + (entitledDays - alreadyReserved) + " day(s) remaining, " + requestedDays + " requested.");
        }

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

    /**
     * Approve or reject a pending leave request.
     *
     * <p>The caller must be the requesting employee's direct manager
     * ({@code employee.managerId == currentUser.id}) or an ADMIN. A manager
     * cannot approve or reject their own request. Only PENDING requests can be
     * transitioned, and only to APPROVED or REJECTED.
     */
    @Transactional
    public UpdateLeaveStatusResponse updateStatus(Long id, UpdateLeaveRequest dto) {
        // 1. Validate non-null inputs
        if (id == null) {
            throw new IllegalArgumentException("Leave request ID cannot be null.");
        }
        if (dto == null || dto.getStatus() == null) {
            throw new IllegalArgumentException("Leave status cannot be null.");
        }
        if (dto.getStatus() != LeaveStatus.APPROVED && dto.getStatus() != LeaveStatus.REJECTED) {
            throw new IllegalArgumentException("Leave status can only be set to APPROVED or REJECTED.");
        }

        // 2. Fetch leave request
        LeaveRequestEntity leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found with ID: " + id));

        // 3. Prevent updating already finalized requests
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Cannot update leave request. Current status is already "
                    + leaveRequest.getStatus());
        }

        // 4. Authorization: only the employee's direct manager or an ADMIN may decide
        EmployeeDetails approver = currentEmployee();
        EmployeeDetails requester = leaveRequest.getEmployee();
        boolean isAdmin = hasAuthority("ADMIN");
        boolean isDirectManager = requester.getManagerId() != null
                && Long.valueOf(requester.getManagerId().longValue()).equals(approver.getId());

        if (!isAdmin && !isDirectManager) {
            throw new AccessDeniedException("You are not the line manager for this leave request.");
        }
        if (approver.getId().equals(requester.getId())) {
            throw new AccessDeniedException("You cannot approve or reject your own leave request.");
        }

        // 5. Handle status transitions and domain logic
        if (dto.getStatus() == LeaveStatus.APPROVED) {
            leaveRequest.setApprovedAt(LocalDateTime.now());
        }
        if (dto.getReason() != null && !dto.getReason().isBlank()) {
            leaveRequest.setRemarks(dto.getReason());
        }
        leaveRequest.setApprover(approver);
        leaveRequest.setStatus(dto.getStatus());

        // 6. Save entity
        LeaveRequestEntity saved = leaveRequestRepository.save(leaveRequest);

        return UpdateLeaveStatusResponse.builder()
                .status(saved.getStatus())
                .reason(saved.getRemarks() != null ? saved.getRemarks() : dto.getReason())
                .approvedBy(displayName(approver))
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

    /**
     * Compute the authenticated user's leave balance for every leave type.
     *
     * <p>Pending requests are reserved: {@code remainingDays} subtracts both
     * APPROVED and PENDING day counts, so applying for leave holds the balance
     * even before the LINE_MANAGER approves it. Rejected requests are ignored.
     *
     * <p>Only requests whose start date falls in the current calendar year are
     * counted, so every employee's balance resets to the full entitlement on
     * 1 January. There is no carry-over of unused days.
     */
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getLeaveBalanceForCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        EmployeeDetails employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found for authenticated user: " + username));

        int currentYear = LocalDate.now().getYear();
        List<LeaveRequestEntity> requests = leaveRequestRepository.findByEmployeeId(employee.getId());

        List<LeaveBalanceResponse> balances = new ArrayList<>();
        for (LeaveTypeEntity leaveType : leaveTypeRepository.findAll()) {
            long approvedDays = 0;
            long pendingDays = 0;
            for (LeaveRequestEntity request : requests) {
                if (request.getLeaveType() == null
                        || !request.getLeaveType().getId().equals(leaveType.getId())) {
                    continue;
                }
                if (request.getStartDate() == null
                        || request.getStartDate().getYear() != currentYear) {
                    continue;
                }
                long days = countDays(request.getStartDate(), request.getEndDate());
                if (request.getStatus() == LeaveStatus.APPROVED) {
                    approvedDays += days;
                } else if (request.getStatus() == LeaveStatus.PENDING) {
                    pendingDays += days;
                }
            }
            int entitledDays = leaveType.getMaxDays() != null ? leaveType.getMaxDays() : 0;
            balances.add(new LeaveBalanceResponse(
                    leaveType.getId(),
                    leaveType.getName(),
                    entitledDays,
                    approvedDays,
                    pendingDays,
                    entitledDays - approvedDays - pendingDays));
        }
        return balances;
    }

    /** Inclusive whole-day count between two dates (start and end both counted). */
    private long countDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * Days already reserved (APPROVED or PENDING) for one leave type in a given
     * year, used to enforce the balance when a new request is created.
     */
    private long reservedDaysForType(Long employeeId, Integer leaveTypeId, int year) {
        return leaveRequestRepository.findByEmployeeId(employeeId).stream()
                .filter(r -> r.getLeaveType() != null && r.getLeaveType().getId().equals(leaveTypeId))
                .filter(r -> r.getStartDate() != null && r.getStartDate().getYear() == year)
                .filter(r -> r.getStatus() == LeaveStatus.APPROVED || r.getStatus() == LeaveStatus.PENDING)
                .mapToLong(r -> countDays(r.getStartDate(), r.getEndDate()))
                .sum();
    }

    /**
     * Leave requests raised by the current manager's direct reports that are
     * still awaiting a decision. An ADMIN sees every pending request.
     */
    @Transactional(readOnly = true)
    public PagedResponse<LeaveRequestResponse> getPendingApprovalsForCurrentManager(Pageable pageable) {
        EmployeeDetails manager = currentEmployee();
        Page<LeaveRequestEntity> page = hasAuthority("ADMIN")
                ? leaveRequestRepository.findByStatus(LeaveStatus.PENDING, pageable)
                : leaveRequestRepository.findByEmployee_ManagerIdAndStatus(
                        manager.getId().intValue(), LeaveStatus.PENDING, pageable);

        List<LeaveRequestResponse> content = page.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return PagedResponse.of(page, content);
    }

    /** The employee behind the current authentication, or an error if none. */
    private EmployeeDetails currentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("No authenticated user.");
        }
        String username = auth.getName();
        return employeeRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found for authenticated user: " + username));
    }

    /** Whether the current authentication carries the given authority. */
    private boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority granted : auth.getAuthorities()) {
            if (authority.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private String displayName(EmployeeDetails employee) {
        String first = employee.getFirstName();
        String last = employee.getLastName();
        if (first != null && last != null) {
            return (first + " " + last).trim();
        }
        if (first != null) {
            return first;
        }
        return employee.getUsername();
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
        


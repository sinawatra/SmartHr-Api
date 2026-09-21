package com.smarthr.smarthr.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.entity.AttendanceEntity;
import com.smarthr.smarthr.entity.DefaultOnboardingTaskEntity;
import com.smarthr.smarthr.entity.DepartmentEntity;
import com.smarthr.smarthr.entity.EmployeeDetails;
import com.smarthr.smarthr.entity.OnboardingTaskEntity;
import com.smarthr.smarthr.entity.RoleEntity;
import com.smarthr.smarthr.enumeration.EmployementStatus;
import com.smarthr.smarthr.exception.InvalidCredentialsException;
import com.smarthr.smarthr.exception.ResourceNotFoundException;
import com.smarthr.smarthr.exception.UserAlreadyExistsException;
import com.smarthr.smarthr.repository.AttendanceRepository;
import com.smarthr.smarthr.repository.CompanyRepository;
import com.smarthr.smarthr.repository.DefaultOnboardingTaskRepository;
import com.smarthr.smarthr.repository.DepartmentRepository;
import com.smarthr.smarthr.repository.EmployeeRepository;
import com.smarthr.smarthr.repository.OnboardingTaskRepository;
import com.smarthr.smarthr.repository.RoleRepository;
import com.smarthr.smarthr.request.CreateEmployeeRequest;
import com.smarthr.smarthr.request.LoginRequest;
import com.smarthr.smarthr.request.RefreshTokenRequest;
import com.smarthr.smarthr.response.EmployeeResponse;
import com.smarthr.smarthr.response.LoginResponse;
import com.smarthr.smarthr.response.OnboardingTaskResponse;
import com.smarthr.smarthr.response.PagedResponse;
import com.smarthr.smarthr.security.JwtTokenProvider;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final JwtTokenProvider tokenProvider;
    private final OnboardingTaskRepository onboardingTaskRepository;
    private final DefaultOnboardingTaskRepository defaultOnboardingTaskRepository;
    private final CompanyRepository companyRepository;
    private final AttendanceRepository attendanceRepository;

    /**
     * Authenticate user/admin by username and password.
     */
    public LoginResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank() ||
            request.getPassword() == null || request.getPassword().isBlank()) {
            throw new InvalidCredentialsException("Username and password are required");
        }

        EmployeeDetails employee = employeeRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), employee.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String roleName = employee.getRole() != null ? employee.getRole().getName() : "USER";
        String token = tokenProvider.generateToken(employee.getUsername(), roleName);

        return LoginResponse.builder()
                .id(employee.getId())
                .username(employee.getUsername())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .token(token)
                .refreshToken(tokenProvider.generateRefreshToken(employee.getUsername()))
                .build();
    }

    /**
     * Issue a new access token (and rotated refresh token) from a valid refresh token.
     */
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        if (refreshToken == null || refreshToken.isBlank() || !tokenProvider.validateRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        EmployeeDetails employee = employeeRepository.findByUsername(tokenProvider.getUsernameFromToken(refreshToken))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));

        String roleName = employee.getRole() != null ? employee.getRole().getName() : "USER";

        return LoginResponse.builder()
                .id(employee.getId())
                .username(employee.getUsername())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .token(tokenProvider.generateToken(employee.getUsername(), roleName))
                .refreshToken(tokenProvider.generateRefreshToken(employee.getUsername()))
                .build();
    }

    private RoleEntity resolveRole(CreateEmployeeRequest request) {
        if (request == null || roleRepository == null) {
            return null;
        }
        if (request.getRoleId() != null) {
            return roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new EntityNotFoundException("Role not found with ID: " + request.getRoleId()));
        }
        return null;
    }

    private DepartmentEntity resolveDepartment(CreateEmployeeRequest request) {
        if (request == null || request.getDepartmentId() == null || departmentRepository == null) {
            return null;
        }

        return departmentRepository.findById(request.getDepartmentId().longValue())
                .orElseThrow(() -> new EntityNotFoundException(
                    "Department not found with ID: " + request.getDepartmentId()
                ));
    }

    private void validateCompany(Integer companyId) {
        if (companyId != null && companyRepository != null) {
            if (!companyRepository.existsById(companyId.longValue())) {
                throw new EntityNotFoundException("Company not found with ID: " + companyId);
            }
        }
    }

    private void validateManager(Integer managerId) {
        if (managerId != null && employeeRepository != null) {
            if (!employeeRepository.existsById(managerId.longValue())) {
                throw new EntityNotFoundException("Manager employee not found with ID: " + managerId);
            }
        }
    }

    private List<OnboardingTaskResponse> getOnboardingTasksForEmployee(Long employeeId) {
        if (onboardingTaskRepository == null || employeeId == null) {
            return null;
        }
        List<OnboardingTaskEntity> tasks = onboardingTaskRepository.findByEmployeeId(employeeId);
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        return tasks.stream()
                .map(OnboardingTaskResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private Map<Long, List<OnboardingTaskResponse>> getOnboardingTasksMapForEmployees(List<Long> employeeIds) {
        if (onboardingTaskRepository == null || employeeIds == null || employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OnboardingTaskEntity> tasks = onboardingTaskRepository.findByEmployeeIdIn(employeeIds);
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyMap();
        }
        return tasks.stream()
                .map(OnboardingTaskResponse::fromEntity)
                .filter(task -> task != null && task.getEmployeeId() != null)
                .collect(Collectors.groupingBy(OnboardingTaskResponse::getEmployeeId));
    }

    /**
     * Admin method to create a new employee with username, password, and assigned role.
     */
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (employeeRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already in use");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank() && employeeRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' is already in use");
        }

        RoleEntity assignedRole = resolveRole(request);
        DepartmentEntity assignedDepartment = resolveDepartment(request);
        validateCompany(request.getCompanyId());
        validateManager(request.getManagerId());

        EmployeeDetails employee = EmployeeDetails.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .companyId(request.getCompanyId())
                .department(assignedDepartment)
                .managerId(request.getManagerId())
                .employeeCode(request.getEmployeeCode())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .employeeStatus(request.getEmployeeStatus() != null ? request.getEmployeeStatus() : EmployementStatus.Probation)
                .hiredate(request.getHiredate())
                .probationEndDate(request.getProbationEndDate())
                .profileImage(request.getProfileImage())
                .telegramUsername(request.getTelegramUsername())
                .telegramChatId(request.getTelegramChatId())
                .endDate(request.getEndDate())
                .build();

        EmployeeDetails savedEmployee = employeeRepository.save(employee);

        List<String> tasksToAssign = request.getOnboardingTasks();
        if ((tasksToAssign == null || tasksToAssign.isEmpty()) && defaultOnboardingTaskRepository != null) {
            List<DefaultOnboardingTaskEntity> defaultTasks = defaultOnboardingTaskRepository.findByActiveTrue();
            if (defaultTasks != null && !defaultTasks.isEmpty()) {
                tasksToAssign = defaultTasks.stream()
                        .map(DefaultOnboardingTaskEntity::getTaskName)
                        .collect(Collectors.toList());
            }
        }

        List<OnboardingTaskResponse> assignedTaskResponses = null;
        if (tasksToAssign != null && !tasksToAssign.isEmpty() && onboardingTaskRepository != null) {
            List<OnboardingTaskEntity> taskEntities = new ArrayList<>();
            for (String taskName : tasksToAssign) {
                if (taskName != null && !taskName.isBlank()) {
                    OnboardingTaskEntity taskEntity = OnboardingTaskEntity.builder()
                            .employee(savedEmployee)
                            .taskName(taskName)
                            .completed(false)
                            .build();
                    taskEntities.add(taskEntity);
                }
            }
            if (!taskEntities.isEmpty()) {
                List<OnboardingTaskEntity> savedTasks = onboardingTaskRepository.saveAll(taskEntities);
                assignedTaskResponses = savedTasks.stream()
                        .map(OnboardingTaskResponse::fromEntity)
                        .collect(Collectors.toList());
            }
        }

        return EmployeeResponse.fromEntity(savedEmployee, assignedTaskResponses);
    }

    /**
     * Retrieve all employees.
     */
    public List<EmployeeResponse> getAllEmployees() {
        List<EmployeeDetails> employees = employeeRepository.findAll();
        if (employees.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> employeeIds = employees.stream().map(EmployeeDetails::getId).collect(Collectors.toList());
        Map<Long, List<OnboardingTaskResponse>> taskMap = getOnboardingTasksMapForEmployees(employeeIds);

        return employees.stream()
                .map(emp -> EmployeeResponse.fromEntity(emp, taskMap.get(emp.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve employees with pagination.
     */
    public PagedResponse<EmployeeResponse> getAllEmployees(Pageable pageable) {
        Page<EmployeeDetails> page = employeeRepository.findAll(pageable);
        List<EmployeeDetails> employees = page.getContent();
        if (employees.isEmpty()) {
            return PagedResponse.of(page, Collections.emptyList());
        }
        List<Long> employeeIds = employees.stream().map(EmployeeDetails::getId).collect(Collectors.toList());
        Map<Long, List<OnboardingTaskResponse>> taskMap = getOnboardingTasksMapForEmployees(employeeIds);

        List<EmployeeResponse> content = employees.stream()
                .map(emp -> EmployeeResponse.fromEntity(emp, taskMap.get(emp.getId())))
                .collect(Collectors.toList());
        return PagedResponse.of(page, content);
    }

    /**
     * Retrieve employee by ID.
     */
    public EmployeeResponse getEmployeeById(Long id) {
        EmployeeDetails employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return EmployeeResponse.fromEntity(employee, getOnboardingTasksForEmployee(employee.getId()));
    }

    /**
     * Retrieve employee by username.
     */
    public EmployeeResponse getEmployeeByUsername(String username) {
        EmployeeDetails employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with username: " + username));
        return EmployeeResponse.fromEntity(employee, getOnboardingTasksForEmployee(employee.getId()));
    }

    /**
     * Update employee.
     */
    public EmployeeResponse updateEmployee(Long id, CreateEmployeeRequest request) {
        EmployeeDetails employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (request.getUsername() != null && !request.getUsername().isBlank() 
                && !request.getUsername().equals(employee.getUsername())) {
            if (employeeRepository.existsByUsername(request.getUsername())) {
                throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already in use");
            }
            employee.setUsername(request.getUsername());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            employee.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        RoleEntity newRole = resolveRole(request);
        if (newRole != null) {
            employee.setRole(newRole);
        }

        DepartmentEntity newDept = resolveDepartment(request);
        if (newDept != null) {
            employee.setDepartment(newDept);
        }

        if (request.getFirstName() != null) employee.setFirstName(request.getFirstName());
        if (request.getLastName() != null) employee.setLastName(request.getLastName());
        if (request.getEmail() != null) employee.setEmail(request.getEmail());
        if (request.getPhoneNumber() != null) employee.setPhoneNumber(request.getPhoneNumber());
        if (request.getEmployeeCode() != null) employee.setEmployeeCode(request.getEmployeeCode());
        if (request.getEmployeeStatus() != null) employee.setEmployeeStatus(request.getEmployeeStatus());
        if (request.getCompanyId() != null) {
            validateCompany(request.getCompanyId());
            employee.setCompanyId(request.getCompanyId());
        }
        if (request.getManagerId() != null) {
            validateManager(request.getManagerId());
            employee.setManagerId(request.getManagerId());
        }
        if (request.getHiredate() != null) employee.setHiredate(request.getHiredate());
        if (request.getProbationEndDate() != null) employee.setProbationEndDate(request.getProbationEndDate());
        if (request.getProfileImage() != null) employee.setProfileImage(request.getProfileImage());
        if (request.getTelegramChatId() != null) employee.setTelegramChatId(request.getTelegramChatId());
        if (request.getTelegramUsername() != null) employee.setTelegramUsername(request.getTelegramUsername());
        if (request.getEndDate() != null) employee.setEndDate(request.getEndDate());

        EmployeeDetails updated = employeeRepository.save(employee);
        return EmployeeResponse.fromEntity(updated, getOnboardingTasksForEmployee(updated.getId()));
    }

    /**
     * Delete employee by ID.
     */
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
    }

    //Get Employement status 
    public String[] getAllEmploymentStatuses() {
        EmployementStatus[] statuses = EmployementStatus.values();
        return Arrays.stream(statuses).map(EmployementStatus::name).toArray(String[]::new);
    }


    /**
     * Retrieve profile info of currently authenticated employee.
     */
    @Transactional(readOnly = true)
    public EmployeeResponse getProfileInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            throw new IllegalStateException("User must be authenticated to get profile info");
        }

        String username = authentication.getName();
        EmployeeDetails employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with username: " + username));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        List<AttendanceEntity> todayAttendances = attendanceRepository
                .findByEmployeeIdAndClockInBetweenOrderByClockInDesc(employee.getId(), startOfDay, endOfDay);

        boolean hasClockIn = !todayAttendances.isEmpty();
        boolean hasClockOut = !todayAttendances.isEmpty() && todayAttendances.get(0).getClockOut() != null;

        Optional<AttendanceEntity> activeSession = attendanceRepository
                .findTopByEmployeeIdAndClockOutIsNullOrderByClockInDesc(employee.getId());
        if (activeSession.isPresent()) {
            hasClockIn = true;
            hasClockOut = false;
        }

        return EmployeeResponse.fromEntity(employee, getOnboardingTasksForEmployee(employee.getId()), hasClockIn, hasClockOut);
    }
}


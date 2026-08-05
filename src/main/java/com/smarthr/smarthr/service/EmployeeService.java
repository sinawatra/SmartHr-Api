package com.smarthr.smarthr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smarthr.smarthr.entity.DefaultOnboardingTaskEntity;
import com.smarthr.smarthr.entity.EmployeeDetails;
import com.smarthr.smarthr.entity.OnboardingTaskEntity;
import com.smarthr.smarthr.entity.RoleEntity;
import com.smarthr.smarthr.exception.InvalidCredentialsException;
import com.smarthr.smarthr.exception.ResourceNotFoundException;
import com.smarthr.smarthr.exception.UserAlreadyExistsException;
import com.smarthr.smarthr.repository.DefaultOnboardingTaskRepository;
import com.smarthr.smarthr.repository.EmployeeRepository;
import com.smarthr.smarthr.repository.OnboardingTaskRepository;
import com.smarthr.smarthr.repository.RoleRepository;
import com.smarthr.smarthr.request.CreateEmployeeRequest;
import com.smarthr.smarthr.request.LoginRequest;
import com.smarthr.smarthr.response.EmployeeResponse;
import com.smarthr.smarthr.response.LoginResponse;
import com.smarthr.smarthr.response.PagedResponse;
import com.smarthr.smarthr.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider tokenProvider;
    private final OnboardingTaskRepository onboardingTaskRepository;
    private final DefaultOnboardingTaskRepository defaultOnboardingTaskRepository;

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
                .build();
    }

    private RoleEntity resolveRole(CreateEmployeeRequest request) {
        if (request == null || roleRepository == null) {
            return null;
        }
        if (request.getRoleId() != null) {
            return roleRepository.findById(request.getRoleId()).orElse(null);
        }
        return null;
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

        EmployeeDetails employee = EmployeeDetails.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .companyId(request.getCompanyId())
                .departmentId(request.getDepartmentId())
                .managerId(request.getManagerId())
                .employeeCode(request.getEmployeeCode())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .employeeStatus(request.getEmployeeStatus())
                .hiredate(request.getHiredate())
                .probationEndDate(request.getProbationEndDate())
                .profileImage(request.getProfileImage())
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

        List<String> assignedTaskNames = null;
        if (tasksToAssign != null && !tasksToAssign.isEmpty() && onboardingTaskRepository != null) {
            assignedTaskNames = new ArrayList<>();
            List<OnboardingTaskEntity> taskEntities = new ArrayList<>();
            for (String taskName : tasksToAssign) {
                if (taskName != null && !taskName.isBlank()) {
                    OnboardingTaskEntity taskEntity = OnboardingTaskEntity.builder()
                            .employee(savedEmployee)
                            .taskName(taskName)
                            .completed(false)
                            .build();
                    taskEntities.add(taskEntity);
                    assignedTaskNames.add(taskName);
                }
            }
            if (!taskEntities.isEmpty()) {
                onboardingTaskRepository.saveAll(taskEntities);
            }
        }

        return EmployeeResponse.fromEntity(savedEmployee, assignedTaskNames);
    }

    /**
     * Retrieve all employees.
     */
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(EmployeeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve employees with pagination.
     */
    public PagedResponse<EmployeeResponse> getAllEmployees(Pageable pageable) {
        Page<EmployeeDetails> page = employeeRepository.findAll(pageable);
        List<EmployeeResponse> content = page.getContent().stream()
                .map(EmployeeResponse::fromEntity)
                .collect(Collectors.toList());
        return PagedResponse.of(page, content);
    }

    /**
     * Retrieve employee by ID.
     */
    public EmployeeResponse getEmployeeById(Long id) {
        EmployeeDetails employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return EmployeeResponse.fromEntity(employee);
    }

    /**
     * Retrieve employee by username.
     */
    public EmployeeResponse getEmployeeByUsername(String username) {
        EmployeeDetails employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with username: " + username));
        return EmployeeResponse.fromEntity(employee);
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

        if (request.getFirstName() != null) employee.setFirstName(request.getFirstName());
        if (request.getLastName() != null) employee.setLastName(request.getLastName());
        if (request.getEmail() != null) employee.setEmail(request.getEmail());
        if (request.getPhoneNumber() != null) employee.setPhoneNumber(request.getPhoneNumber());
        if (request.getEmployeeCode() != null) employee.setEmployeeCode(request.getEmployeeCode());
        if (request.getEmployeeStatus() != null) employee.setEmployeeStatus(request.getEmployeeStatus());
        if (request.getCompanyId() != null) employee.setCompanyId(request.getCompanyId());
        if (request.getDepartmentId() != null) employee.setDepartmentId(request.getDepartmentId());
        if (request.getManagerId() != null) employee.setManagerId(request.getManagerId());
        if (request.getHiredate() != null) employee.setHiredate(request.getHiredate());
        if (request.getProbationEndDate() != null) employee.setProbationEndDate(request.getProbationEndDate());
        if (request.getProfileImage() != null) employee.setProfileImage(request.getProfileImage());
        if (request.getEndDate() != null) employee.setEndDate(request.getEndDate());

        EmployeeDetails updated = employeeRepository.save(employee);
        return EmployeeResponse.fromEntity(updated);
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
}

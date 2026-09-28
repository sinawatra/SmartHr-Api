package com.smarthr.smarthr.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smarthr.smarthr.request.CreateEmployeeRequest;
import com.smarthr.smarthr.response.ApiResponse;
import com.smarthr.smarthr.response.EmployeeResponse;
import com.smarthr.smarthr.response.PagedResponse;
import com.smarthr.smarthr.response.ProbationEmployeeResponse;
import com.smarthr.smarthr.response.ProbationSummaryResponse;
import com.smarthr.smarthr.service.EmployeeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * Endpoint for Admin to create an employee or admin account.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(@RequestBody CreateEmployeeRequest request) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee created successfully", response));
    }

    /**
     * Endpoint to list all employees with pagination.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<EmployeeResponse>>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<EmployeeResponse> employees = employeeService.getAllEmployees(pageable);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    /**
     * Endpoint to list employees still under probation, soonest probation end date first by default.
     */
    @GetMapping("/probation")
    public ResponseEntity<ApiResponse<PagedResponse<ProbationEmployeeResponse>>> getProbationEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "probationEndDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<ProbationEmployeeResponse> employees = employeeService.getProbationEmployees(pageable);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    /**
     * Endpoint for probation dashboard counts.
     */
    @GetMapping("/probation/summary")
    public ResponseEntity<ApiResponse<ProbationSummaryResponse>> getProbationSummary() {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getProbationSummary()));
    }

    /**
     * Endpoint to get employee by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeById(@PathVariable Long id) {
        EmployeeResponse response = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Endpoint to update employee details.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable Long id, 
            @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse response = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", response));
    }

    /**
     * Endpoint to delete an employee.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deleted successfully", null));
    }


    //Get all Employment Status
    @GetMapping("/employment-statuses")
    public ResponseEntity<ApiResponse<String[]>> getAllEmploymentStatuses() {
        String[] statuses = employeeService.getAllEmploymentStatuses();
        return ResponseEntity.ok(ApiResponse.success(statuses));}
}

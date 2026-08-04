package com.smarthr.smarthr.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.entity.CompanyEntity;
import com.smarthr.smarthr.entity.DepartmentEntity;
import com.smarthr.smarthr.repository.CompanyRepository;
import com.smarthr.smarthr.repository.DepartmentRepository;
import com.smarthr.smarthr.request.DepartmentRequest;
import com.smarthr.smarthr.response.DepartmentResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Department name is required");
        }

        if (departmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Department with name '" + request.getName() + "' already exists");
        }

        CompanyEntity company = null;
        if (request.getCompanyId() != null) {
            company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + request.getCompanyId()));
        }

        DepartmentEntity entity = DepartmentEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .company(company)
                .build();

        DepartmentEntity saved = departmentRepository.save(entity);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + id));
        return mapToResponse(entity);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            boolean isDifferentName = entity.getName() == null || !entity.getName().equalsIgnoreCase(request.getName());
            if (isDifferentName && departmentRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("Department with name '" + request.getName() + "' already exists");
            }
            entity.setName(request.getName());
        }

        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }

        if (request.getCompanyId() != null) {
            CompanyEntity company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + request.getCompanyId()));
            entity.setCompany(company);
        }

        DepartmentEntity updated = departmentRepository.save(entity);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + id));
        departmentRepository.delete(entity);
    }

    public DepartmentResponse mapToResponse(DepartmentEntity entity) {
        return DepartmentResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .companyId(entity.getCompany() != null ? entity.getCompany().getId() : null)
                .companyName(entity.getCompany() != null ? entity.getCompany().getName() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}


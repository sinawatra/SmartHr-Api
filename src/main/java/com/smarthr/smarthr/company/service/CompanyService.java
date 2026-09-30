package com.smarthr.smarthr.company.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.company.entity.CompanyEntity;
import com.smarthr.smarthr.company.repository.CompanyRepository;
import com.smarthr.smarthr.company.dto.CompanyRequest;
import com.smarthr.smarthr.company.dto.CompanyResponse;
import com.smarthr.smarthr.department.dto.DepartmentResponse;
import com.smarthr.smarthr.common.dto.PagedResponse;

import lombok.RequiredArgsConstructor;
import com.smarthr.smarthr.department.service.DepartmentService;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final DepartmentService departmentService;

    @Transactional
    public CompanyResponse createCompany(CompanyRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Company name is required");
        }

        if (companyRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Company with name '" + request.getName() + "' already exists");
        }

        //TelegramChatId
        if (request.getTelegramChatId() != null && request.getTelegramChatId().isBlank()) {
            throw new IllegalArgumentException("Telegram Chat ID cannot be blank");
        }

        CompanyEntity entity = CompanyEntity.builder()
                .name(request.getName())
                .code(request.getCode())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .longitude(request.getLongitude())
                .latitude(request.getLatitute())
                .telegramChatId(request.getTelegramChatId())
                .build();

        CompanyEntity saved = companyRepository.save(entity);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagedResponse<CompanyResponse> getAllCompanies(Pageable pageable) {
        Page<CompanyEntity> page = companyRepository.findAll(pageable);
        List<CompanyResponse> content = page.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return PagedResponse.of(page, content);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {
        CompanyEntity entity = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + id));
        return mapToResponse(entity);
    }

    @Transactional
    public CompanyResponse updateCompany(Long id, CompanyRequest request) {
        CompanyEntity entity = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            if (!entity.getName().equalsIgnoreCase(request.getName()) && companyRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("Company with name '" + request.getName() + "' already exists");
            }
            entity.setName(request.getName());
        }

        if (request.getCode() != null) entity.setCode(request.getCode());
        if (request.getAddress() != null) entity.setAddress(request.getAddress());
        if (request.getPhone() != null) entity.setPhone(request.getPhone());
        if (request.getEmail() != null) entity.setEmail(request.getEmail());
        if (request.getTelegramChatId() != null) entity.setTelegramChatId(request.getTelegramChatId());
        if (request.getLongitude() != null) entity.setLongitude(request.getLongitude());
        if (request.getLatitute() != null) entity.setLatitude(request.getLatitute());

        CompanyEntity updated = companyRepository.save(entity);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteCompany(Long id) {
        CompanyEntity entity = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + id));
        companyRepository.delete(entity);
    }

    private CompanyResponse mapToResponse(CompanyEntity entity) {
        List<DepartmentResponse> departmentResponses = entity.getDepartments() != null
                ? entity.getDepartments().stream()
                        .map(departmentService::mapToResponse)
                        .collect(Collectors.toList())
                : List.of();

        return CompanyResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .address(entity.getAddress())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .longitude(entity.getLongitude())
                .latitude(entity.getLatitude())
                .telegramChatId(entity.getTelegramChatId())
                .departments(departmentResponses)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

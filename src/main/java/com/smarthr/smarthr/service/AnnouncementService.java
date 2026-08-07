package com.smarthr.smarthr.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.entity.AnnouncementEntity;
import com.smarthr.smarthr.entity.CompanyEntity;
import com.smarthr.smarthr.entity.DepartmentEntity;
import com.smarthr.smarthr.entity.EmployeeDetails;
import com.smarthr.smarthr.exception.ResourceNotFoundException;
import com.smarthr.smarthr.repository.AnnouncementRepository;
import com.smarthr.smarthr.repository.CompanyRepository;
import com.smarthr.smarthr.repository.DepartmentRepository;
import com.smarthr.smarthr.repository.EmployeeRepository;
import com.smarthr.smarthr.request.AnnouncementRequest;
import com.smarthr.smarthr.response.AnnouncementResponse;
import com.smarthr.smarthr.response.PagedResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * Create announcement for current authenticated user obtained via SecurityContextHolder.
     */
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User must be authenticated to create an announcement");
        }

        String username = authentication.getName();
        EmployeeDetails currentEmployee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with username: " + username));

        if (request.getCompanyId() != null) {
            Integer creatorCompanyId = currentEmployee.getCompanyId();
            if (creatorCompanyId == null || !request.getCompanyId().equals(creatorCompanyId.longValue())) {
                throw new IllegalArgumentException("Creation failed: The specified company ID does not match your assigned company ID.");
            }
        }

        CompanyEntity company = resolveCompany(request.getCompanyId());
        DepartmentEntity department = resolveDepartment(request.getDepartmentId());

        AnnouncementEntity announcement = AnnouncementEntity.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .createdBy(currentEmployee)
                .company(company)
                .department(department)
                .build();

        AnnouncementEntity saved = announcementRepository.save(announcement);
        return AnnouncementResponse.fromEntity(saved);
    }

    /**
     * Retrieve all announcements.
     */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAllAnnouncements() {
        return announcementRepository.findAll().stream()
                .map(AnnouncementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve announcements with pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AnnouncementResponse> getAllAnnouncements(Pageable pageable) {
        Page<AnnouncementEntity> page = announcementRepository.findAll(pageable);
        List<AnnouncementResponse> content = page.getContent().stream()
                .map(AnnouncementResponse::fromEntity)
                .collect(Collectors.toList());
        return PagedResponse.of(page, content);
    }

    /**
     * Retrieve announcement by ID.
     */
    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncementById(Long id) {
        AnnouncementEntity announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));
        return AnnouncementResponse.fromEntity(announcement);
    }

    /**
     * Update announcement.
     */
    public AnnouncementResponse updateAnnouncement(Long id, AnnouncementRequest request) {
        AnnouncementEntity announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            announcement.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            announcement.setDescription(request.getDescription());
        }
        if (request.getCompanyId() != null) {
            announcement.setCompany(resolveCompany(request.getCompanyId()));
        }
        if (request.getDepartmentId() != null) {
            announcement.setDepartment(resolveDepartment(request.getDepartmentId()));
        }

        AnnouncementEntity updated = announcementRepository.save(announcement);
        return AnnouncementResponse.fromEntity(updated);
    }

    /**
     * Delete announcement by ID.
     */
    public void deleteAnnouncement(Long id) {
        if (!announcementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Announcement not found with id: " + id);
        }
        announcementRepository.deleteById(id);
    }

    private CompanyEntity resolveCompany(Long companyId) {
        if (companyId == null) {
            return null;
        }
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + companyId));
    }

    private DepartmentEntity resolveDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));
    }
}

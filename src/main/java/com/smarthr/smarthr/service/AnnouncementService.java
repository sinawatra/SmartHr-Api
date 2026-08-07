package com.smarthr.smarthr.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import com.smarthr.smarthr.entity.AnnouncementEntity;
import com.smarthr.smarthr.entity.EmployeeDetails;
import com.smarthr.smarthr.exception.ResourceNotFoundException;
import com.smarthr.smarthr.repository.AnnouncementRepository;
import com.smarthr.smarthr.repository.EmployeeRepository;
import com.smarthr.smarthr.request.AnnouncementRequest;
import com.smarthr.smarthr.response.AnnouncementResponse;
import com.smarthr.smarthr.response.PagedResponse;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final EmployeeRepository employeeRepository;

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

        AnnouncementEntity announcement = AnnouncementEntity.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .createdBy(currentEmployee)
                .build();

        AnnouncementEntity saved = announcementRepository.save(announcement);
        return AnnouncementResponse.fromEntity(saved);
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
    public AnnouncementResponse getAnnouncementById(@PathVariable @NotNull @Positive Long id) {

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
}

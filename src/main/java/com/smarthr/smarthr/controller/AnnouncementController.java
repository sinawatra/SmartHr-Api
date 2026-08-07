package com.smarthr.smarthr.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smarthr.smarthr.request.AnnouncementRequest;
import com.smarthr.smarthr.response.ApiResponse;
import com.smarthr.smarthr.response.AnnouncementResponse;
import com.smarthr.smarthr.response.PagedResponse;
import com.smarthr.smarthr.service.AnnouncementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/v1/announcements", "/api/v1/announcements/"})
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;


    @GetMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<AnnouncementResponse>>> getAnnouncementsPaged(Pageable pageable) {
        PagedResponse<AnnouncementResponse> response = announcementService.getAllAnnouncements(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncementById(@PathVariable Long id) {
        AnnouncementResponse response = announcementService.getAnnouncementById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(@RequestBody AnnouncementRequest request) {
        AnnouncementResponse response = announcementService.createAnnouncement(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Announcement created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody AnnouncementRequest request) {
        AnnouncementResponse response = announcementService.updateAnnouncement(id, request);
        return ResponseEntity.ok(ApiResponse.success("Announcement updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success("Announcement deleted successfully", null));
    }
}

package com.smarthr.smarthr.notification.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.common.dto.PagedResponse;
import com.smarthr.smarthr.common.exception.ResourceNotFoundException;
import com.smarthr.smarthr.employee.entity.EmployeeDetails;
import com.smarthr.smarthr.employee.repository.EmployeeRepository;
import com.smarthr.smarthr.notification.dto.NotificationResponse;
import com.smarthr.smarthr.notification.entity.NotificationEntity;
import com.smarthr.smarthr.notification.entity.NotificationType;
import com.smarthr.smarthr.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Creates in-app notifications and serves them to the logged-in employee.
 *
 * <p>The notify methods join the caller's transaction, so a notification is
 * only stored if the change it describes (a leave decision, an evaluation...) is
 * committed too.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    private final Clock clock;

    /** Notify one employee. Does nothing if the recipient is null. */
    @Transactional
    public void notify(EmployeeDetails recipient, NotificationType type, String title, String message, Long referenceId) {
        if (recipient == null) {
            return;
        }
        notificationRepository.save(build(recipient, type, title, message, referenceId));
    }

    /** Notify several employees at once, skipping nulls and duplicates. */
    @Transactional
    public void notifyAll(Collection<EmployeeDetails> recipients, NotificationType type, String title, String message, Long referenceId) {
        Map<Long, EmployeeDetails> unique = new LinkedHashMap<>();
        for (EmployeeDetails recipient : recipients) {
            if (recipient != null && recipient.getId() != null) {
                unique.putIfAbsent(recipient.getId(), recipient);
            }
        }
        if (unique.isEmpty()) {
            return;
        }
        notificationRepository.saveAll(unique.values().stream()
                .map(recipient -> build(recipient, type, title, message, referenceId))
                .collect(Collectors.toList()));
    }

    /** Notify the employee's direct manager. Does nothing if they have none. */
    @Transactional
    public void notifyManagerOf(EmployeeDetails employee, NotificationType type, String title, String message, Long referenceId) {
        if (employee.getManagerId() == null) {
            return;
        }
        employeeRepository.findById(employee.getManagerId().longValue())
                .filter(manager -> !manager.getId().equals(employee.getId()))
                .ifPresent(manager -> notify(manager, type, title, message, referenceId));
    }

    /** Notify every employee with the given role, except {@code excludeEmployeeId} (may be null). */
    @Transactional
    public void notifyRole(String roleName, Long excludeEmployeeId, NotificationType type, String title, String message, Long referenceId) {
        List<EmployeeDetails> recipients = employeeRepository.findByRole_Name(roleName).stream()
                .filter(employee -> !employee.getId().equals(excludeEmployeeId))
                .collect(Collectors.toList());
        notifyAll(recipients, type, title, message, referenceId);
    }

    /** The current user's notifications, newest first. */
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getMyNotifications(boolean unreadOnly, Pageable pageable) {
        Long recipientId = currentEmployee().getId();
        Page<NotificationEntity> page = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalse(recipientId, pageable)
                : notificationRepository.findByRecipientId(recipientId, pageable);
        List<NotificationResponse> content = page.getContent().stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
        return PagedResponse.of(page, content);
    }

    @Transactional(readOnly = true)
    public long getMyUnreadCount() {
        return notificationRepository.countByRecipientIdAndReadFalse(currentEmployee().getId());
    }

    @Transactional
    public NotificationResponse markAsRead(Long id) {
        NotificationEntity notification = notificationRepository.findByIdAndRecipientId(id, currentEmployee().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + id));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now(clock));
        }
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    /** Marks all of the current user's notifications as read and returns how many changed. */
    @Transactional
    public int markAllAsRead() {
        return notificationRepository.markAllAsRead(currentEmployee().getId(), Instant.now(clock));
    }

    private NotificationEntity build(EmployeeDetails recipient, NotificationType type, String title, String message, Long referenceId) {
        return NotificationEntity.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .build();
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
}

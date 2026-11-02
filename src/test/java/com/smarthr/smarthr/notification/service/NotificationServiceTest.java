package com.smarthr.smarthr.notification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.smarthr.smarthr.common.exception.ResourceNotFoundException;
import com.smarthr.smarthr.employee.entity.EmployeeDetails;
import com.smarthr.smarthr.employee.repository.EmployeeRepository;
import com.smarthr.smarthr.notification.dto.NotificationResponse;
import com.smarthr.smarthr.notification.entity.NotificationEntity;
import com.smarthr.smarthr.notification.entity.NotificationType;
import com.smarthr.smarthr.notification.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-19T10:00:00Z"), ZoneOffset.UTC);

    private NotificationService notificationService;

    private EmployeeDetails employee;
    private EmployeeDetails manager;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, employeeRepository, clock);
        employee = EmployeeDetails.builder().id(1L).username("john_doe").managerId(2).build();
        manager = EmployeeDetails.builder().id(2L).username("manager").build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john_doe", null, List.of()));
        lenient().when(employeeRepository.findByUsername("john_doe")).thenReturn(Optional.of(employee));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testNotify_SavesNotificationForRecipient() {
        notificationService.notify(employee, NotificationType.LEAVE_APPROVED, "Leave request approved", "Approved", 5L);

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        NotificationEntity saved = captor.getValue();
        assertSame(employee, saved.getRecipient());
        assertEquals(NotificationType.LEAVE_APPROVED, saved.getType());
        assertEquals(5L, saved.getReferenceId());
        assertFalse(saved.isRead());
    }

    @Test
    void testNotify_NullRecipientIsIgnored() {
        notificationService.notify(null, NotificationType.LEAVE_APPROVED, "t", "m", 5L);

        verifyNoInteractions(notificationRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testNotifyAll_SkipsNullsAndDuplicates() {
        EmployeeDetails sameEmployeeAgain = EmployeeDetails.builder().id(1L).username("john_doe").build();

        notificationService.notifyAll(Arrays.asList(employee, null, sameEmployeeAgain, manager),
                NotificationType.ANNOUNCEMENT, "Title", "Body", 7L);

        ArgumentCaptor<List<NotificationEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    void testNotifyManagerOf_NotifiesDirectManager() {
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(manager));

        notificationService.notifyManagerOf(employee, NotificationType.LEAVE_REQUESTED, "New leave request", "m", 5L);

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        assertSame(manager, captor.getValue().getRecipient());
    }

    @Test
    void testNotifyManagerOf_NoManagerDoesNothing() {
        employee.setManagerId(null);

        notificationService.notifyManagerOf(employee, NotificationType.LEAVE_REQUESTED, "t", "m", 5L);

        verifyNoInteractions(notificationRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testNotifyRole_ExcludesGivenEmployee() {
        EmployeeDetails hr = EmployeeDetails.builder().id(3L).username("hr").build();
        when(employeeRepository.findByRole_Name("HR")).thenReturn(List.of(employee, hr));

        notificationService.notifyRole("HR", 1L, NotificationType.PROBATION_EVALUATION_SUBMITTED, "t", "m", 10L);

        ArgumentCaptor<List<NotificationEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertSame(hr, captor.getValue().get(0).getRecipient());
    }

    @Test
    void testMarkAsRead_SetsReadAndTimestamp() {
        NotificationEntity notification = NotificationEntity.builder().id(9L).recipient(employee)
                .type(NotificationType.ANNOUNCEMENT).title("t").build();
        when(notificationRepository.findByIdAndRecipientId(9L, 1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(9L);

        assertTrue(response.isHasRead());
        assertEquals(Instant.parse("2026-10-19T10:00:00Z"), response.getReadAt());
    }

    @Test
    void testMarkAsRead_OtherEmployeesNotificationIsNotFound() {
        when(notificationRepository.findByIdAndRecipientId(9L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(9L));
    }

    @Test
    void testGetMyUnreadCount() {
        when(notificationRepository.countByRecipientIdAndReadFalse(1L)).thenReturn(4L);

        assertEquals(4L, notificationService.getMyUnreadCount());
    }
}

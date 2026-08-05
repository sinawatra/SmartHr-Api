package com.smarthr.smarthr.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smarthr.smarthr.entity.EmployeeDetails;
import com.smarthr.smarthr.entity.OnboardingTaskEntity;
import com.smarthr.smarthr.repository.OnboardingTaskRepository;
import com.smarthr.smarthr.response.OnboardingTaskResponse;

@ExtendWith(MockitoExtension.class)
class OnboardingTaskServiceTest {

    @Mock
    private OnboardingTaskRepository onboardingTaskRepository;

    @InjectMocks
    private OnboardingTaskService onboardingTaskService;

    @Test
    void testGetTasksByEmployeeId() {
        EmployeeDetails employee = EmployeeDetails.builder().id(10L).username("testuser").build();
        OnboardingTaskEntity task1 = OnboardingTaskEntity.builder().id(1).employee(employee).taskName("Birth Certificate").completed(false).build();
        OnboardingTaskEntity task2 = OnboardingTaskEntity.builder().id(2).employee(employee).taskName("Employment Certificate").completed(false).build();

        when(onboardingTaskRepository.findByEmployeeId(10L)).thenReturn(List.of(task1, task2));

        List<OnboardingTaskResponse> result = onboardingTaskService.getTasksByEmployeeId(10L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Birth Certificate", result.get(0).getTaskName());
        assertFalse(result.get(0).isCompleted());
    }

    @Test
    void testUpdateTaskCompletion_TickTask() {
        EmployeeDetails employee = EmployeeDetails.builder().id(10L).username("testuser").build();
        OnboardingTaskEntity task = OnboardingTaskEntity.builder().id(1).employee(employee).taskName("Birth Certificate").completed(false).build();

        when(onboardingTaskRepository.findById(1)).thenReturn(Optional.of(task));
        when(onboardingTaskRepository.save(any(OnboardingTaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OnboardingTaskResponse response = onboardingTaskService.updateTaskCompletion(1, true);

        assertNotNull(response);
        assertTrue(response.isCompleted());
        assertNotNull(response.getCompletedAt());
        verify(onboardingTaskRepository, times(1)).save(task);
    }
}

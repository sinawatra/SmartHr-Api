package com.smarthr.smarthr.config;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smarthr.smarthr.repository.DefaultOnboardingTaskRepository;

@ExtendWith(MockitoExtension.class)
class OnboardingTaskDataInitializerTest {

    @Mock
    private DefaultOnboardingTaskRepository repository;

    @InjectMocks
    private OnboardingTaskDataInitializer initializer;

    @Test
    void testRun_WhenRepositoryIsEmpty_SeedsSampleTasks() {
        when(repository.count()).thenReturn(0L);

        initializer.run();

        verify(repository, times(1)).saveAll(anyList());
    }

    @Test
    void testRun_WhenRepositoryIsNotEmpty_DoesNotSeed() {
        when(repository.count()).thenReturn(5L);

        initializer.run();

        verify(repository, never()).saveAll(anyList());
    }
}

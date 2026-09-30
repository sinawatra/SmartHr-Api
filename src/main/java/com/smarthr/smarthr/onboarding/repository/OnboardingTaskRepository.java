package com.smarthr.smarthr.onboarding.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smarthr.smarthr.onboarding.entity.OnboardingTaskEntity;

@Repository
public interface OnboardingTaskRepository extends JpaRepository<OnboardingTaskEntity, Integer> {
    List<OnboardingTaskEntity> findByEmployeeId(Long employeeId);
    List<OnboardingTaskEntity> findByEmployeeIdIn(List<Long> employeeIds);
}

package com.smarthr.smarthr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smarthr.smarthr.entity.DefaultOnboardingTaskEntity;

@Repository
public interface DefaultOnboardingTaskRepository extends JpaRepository<DefaultOnboardingTaskEntity, Integer> {
    List<DefaultOnboardingTaskEntity> findByActiveTrue();
}

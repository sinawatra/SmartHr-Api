package com.smarthr.smarthr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smarthr.smarthr.entity.AnnouncementEntity;

@Repository
public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, Long> {
}

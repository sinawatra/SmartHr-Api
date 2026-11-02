package com.smarthr.smarthr.notification.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smarthr.smarthr.notification.entity.NotificationEntity;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Page<NotificationEntity> findByRecipientId(Long recipientId, Pageable pageable);
    Page<NotificationEntity> findByRecipientIdAndReadFalse(Long recipientId, Pageable pageable);
    long countByRecipientIdAndReadFalse(Long recipientId);

    // Scoped to the recipient so one employee can never read or update another's notification
    Optional<NotificationEntity> findByIdAndRecipientId(Long id, Long recipientId);

    @Modifying
    @Query("update NotificationEntity n set n.read = true, n.readAt = :readAt "
            + "where n.recipient.id = :recipientId and n.read = false")
    int markAllAsRead(@Param("recipientId") Long recipientId, @Param("readAt") Instant readAt);
}

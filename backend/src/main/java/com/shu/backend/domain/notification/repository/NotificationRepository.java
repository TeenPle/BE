package com.shu.backend.domain.notification.repository;

import com.shu.backend.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Slice<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.userId = :userId and n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Notification n where n.targetType = :targetType and n.targetId = :targetId")
    void deleteAllByTargetTypeAndTargetId(
            @Param("targetType") com.shu.backend.domain.notification.enums.NotificationTargetType targetType,
            @Param("targetId") Long targetId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Notification n where n.targetType = :targetType and n.targetId in :targetIds")
    void deleteAllByTargetTypeAndTargetIdIn(
            @Param("targetType") com.shu.backend.domain.notification.enums.NotificationTargetType targetType,
            @Param("targetIds") List<Long> targetIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Notification n where n.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}

package com.example.Insurance.repository;

import com.example.Insurance.entity.UserNotification;
import com.example.Insurance.entity.UserNotification.NotificationType;
import com.example.Insurance.entity.UserNotification.PriorityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    // Find all active notifications for a user
    @Query("SELECT n FROM UserNotification n WHERE n.userId = :userId AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<UserNotification> findByUserIdAndActiveOrderByCreatedAtDesc(@Param("userId") Long userId);

    // Find unread notifications for a user
    @Query("SELECT n FROM UserNotification n WHERE n.userId = :userId AND n.isRead = false AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<UserNotification> findByUserIdAndUnreadOrderByCreatedAtDesc(@Param("userId") Long userId);

    // Find read notifications for a user
    @Query("SELECT n FROM UserNotification n WHERE n.userId = :userId AND n.isRead = true AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<UserNotification> findByUserIdAndReadOrderByCreatedAtDesc(@Param("userId") Long userId);

    // Find notifications by priority for a user
    @Query("SELECT n FROM UserNotification n WHERE n.userId = :userId AND n.priority = :priority AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<UserNotification> findByUserIdAndPriorityOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("priority") PriorityLevel priority);

    // Find notifications by type for a user
    @Query("SELECT n FROM UserNotification n WHERE n.userId = :userId AND n.type = :type AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<UserNotification> findByUserIdAndTypeOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("type") NotificationType type);

    // Find archived notifications for a user
    @Query("SELECT n FROM UserNotification n WHERE n.userId = :userId AND n.isArchived = true AND n.isDeleted = false ORDER BY n.archivedAt DESC")
    List<UserNotification> findByUserIdAndArchivedOrderByArchivedAtDesc(@Param("userId") Long userId);

    // Search notifications by keyword
    @Query("SELECT n FROM UserNotification n WHERE n.userId = :userId AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(n.message) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<UserNotification> searchByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    // Find notifications by date range
    @Query("SELECT n FROM UserNotification n WHERE n.userId = :userId AND n.createdAt BETWEEN :startDate AND :endDate AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<UserNotification> findByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Count unread notifications for a user
    @Query("SELECT COUNT(n) FROM UserNotification n WHERE n.userId = :userId AND n.isRead = false AND n.isDeleted = false")
    Long countUnreadByUserId(@Param("userId") Long userId);

    // Mark notification as read
    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true, n.readAt = :readTime WHERE n.id = :id AND n.userId = :userId")
    void markAsRead(@Param("id") Long id, @Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);

    // Mark notification as unread
    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = false, n.readAt = null WHERE n.id = :id AND n.userId = :userId")
    void markAsUnread(@Param("id") Long id, @Param("userId") Long userId);

    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true, n.readAt = :readTime WHERE n.userId = :userId AND n.isRead = false AND n.isDeleted = false")
    void markAllAsReadByUserId(@Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);

    // Soft delete notification
    @Modifying
    @Query("UPDATE UserNotification n SET n.isDeleted = true, n.deletedAt = :deleteTime WHERE n.id = :id AND n.userId = :userId")
    void softDeleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId, @Param("deleteTime") LocalDateTime deleteTime);

    // Soft delete all notifications for a user
    @Modifying
    @Query("UPDATE UserNotification n SET n.isDeleted = true, n.deletedAt = :deleteTime WHERE n.userId = :userId AND n.isDeleted = false")
    void softDeleteAllByUserId(@Param("userId") Long userId, @Param("deleteTime") LocalDateTime deleteTime);

    // Archive notification
    @Modifying
    @Query("UPDATE UserNotification n SET n.isArchived = true, n.archivedAt = :archiveTime WHERE n.id = :id AND n.userId = :userId")
    void archiveByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId, @Param("archiveTime") LocalDateTime archiveTime);

    // Find active notification by ID and user ID
    @Query("SELECT n FROM UserNotification n WHERE n.id = :id AND n.userId = :userId AND n.isDeleted = false")
    Optional<UserNotification> findByIdAndUserIdAndActive(@Param("id") Long id, @Param("userId") Long userId);

    // Check if notification exists for admin notification and user
    @Query("SELECT n FROM UserNotification n WHERE n.adminNotificationId = :adminNotificationId AND n.userId = :userId")
    Optional<UserNotification> findByAdminNotificationIdAndUserId(@Param("adminNotificationId") Long adminNotificationId, @Param("userId") Long userId);

    // Find expired notifications for cleanup
    @Query("SELECT n FROM UserNotification n WHERE n.expiryDate IS NOT NULL AND n.expiryDate < :currentTime AND n.isDeleted = false")
    List<UserNotification> findExpiredNotifications(@Param("currentTime") LocalDateTime currentTime);

    // SIMPLIFIED Statistics queries (no date functions)
    @Query("SELECT COUNT(n) FROM UserNotification n WHERE n.userId = :userId AND n.isDeleted = false")
    Long countTotalByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM UserNotification n WHERE n.userId = :userId AND n.isRead = false AND n.isDeleted = false")
    Long countUnreadNotificationsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM UserNotification n WHERE n.userId = :userId AND n.priority = 'URGENT' AND n.isDeleted = false")
    Long countUrgentByUserId(@Param("userId") Long userId);

    // SIMPLIFIED: Remove date function, calculate in service layer
    @Query("SELECT COUNT(n) FROM UserNotification n WHERE n.userId = :userId AND n.isDeleted = false")
    Long countAllByUserId(@Param("userId") Long userId);
}

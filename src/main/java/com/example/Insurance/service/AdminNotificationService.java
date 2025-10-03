package com.example.Insurance.service;

import com.example.Insurance.DTO.AdminNotificationDTO;
import com.example.Insurance.entity.AdminNotification;
import com.example.Insurance.entity.UserNotification;
import com.example.Insurance.repository.AdminNotificationRepository;
import com.example.Insurance.repository.UserNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminNotificationService {

    @Autowired
    private AdminNotificationRepository adminNotificationRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    private final Random random = new Random();

    // Create new notification
    public AdminNotificationDTO createNotification(AdminNotificationDTO notificationDTO) {
        AdminNotification notification = convertToEntity(notificationDTO);
        notification.setStatus(AdminNotification.NotificationStatus.DRAFT);
        notification.setCreatedBy("admin");

        AdminNotification savedNotification = adminNotificationRepository.save(notification);
        return convertToDTO(savedNotification);
    }

    // Get all active notifications
    public List<AdminNotificationDTO> getAllNotifications() {
        List<AdminNotification> notifications = adminNotificationRepository.findByIsActiveTrueOrderByCreatedDesc();
        return notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get notification by ID
    public Optional<AdminNotificationDTO> getNotificationById(Long id) {
        Optional<AdminNotification> notification = adminNotificationRepository.findByIdAndIsActiveTrue(id);
        return notification.map(this::convertToDTO);
    }

    // Update notification
    public Optional<AdminNotificationDTO> updateNotification(Long id, AdminNotificationDTO notificationDTO) {
        Optional<AdminNotification> existingNotification = adminNotificationRepository.findByIdAndIsActiveTrue(id);

        if (existingNotification.isPresent()) {
            AdminNotification notification = existingNotification.get();

            notification.setTitle(notificationDTO.getTitle());
            notification.setMessage(notificationDTO.getMessage());
            notification.setType(notificationDTO.getType());
            notification.setPriority(notificationDTO.getPriority());
            notification.setTarget(notificationDTO.getTarget());
            notification.setScheduleDate(notificationDTO.getScheduleDate());
            notification.setExpiryDate(notificationDTO.getExpiryDate());
            notification.setUpdatedDate(LocalDateTime.now());

            AdminNotification updatedNotification = adminNotificationRepository.save(notification);
            return Optional.of(convertToDTO(updatedNotification));
        }

        return Optional.empty();
    }

    // Soft delete notification
    public boolean deleteNotification(Long id) {
        if (adminNotificationRepository.existsById(id)) {
            adminNotificationRepository.softDeleteById(id);
            return true;
        }
        return false;
    }

    // Send notification immediately with user sync
    public Optional<AdminNotificationDTO> sendNotification(Long id) {
        Optional<AdminNotification> notificationOpt = adminNotificationRepository.findByIdAndIsActiveTrue(id);

        if (notificationOpt.isPresent()) {
            AdminNotification notification = notificationOpt.get();

            int sentCount = simulateNotificationSending(notification.getTarget());

            notification.setStatus(AdminNotification.NotificationStatus.SENT);
            notification.setSentCount(sentCount);
            notification.setUpdatedDate(LocalDateTime.now());

            AdminNotification updatedNotification = adminNotificationRepository.save(notification);

            // Sync to user notifications
            syncNotificationToUsers(updatedNotification);

            return Optional.of(convertToDTO(updatedNotification));
        }

        return Optional.empty();
    }

    // Send new notification with user sync
    public AdminNotificationDTO sendNewNotification(AdminNotificationDTO notificationDTO) {
        AdminNotification notification = convertToEntity(notificationDTO);

        int sentCount = simulateNotificationSending(notification.getTarget());

        notification.setStatus(AdminNotification.NotificationStatus.SENT);
        notification.setSentCount(sentCount);
        notification.setCreatedBy("admin");

        AdminNotification savedNotification = adminNotificationRepository.save(notification);

        // Sync to user notifications
        syncNotificationToUsers(savedNotification);

        return convertToDTO(savedNotification);
    }

    // Schedule notification
    public AdminNotificationDTO scheduleNotification(AdminNotificationDTO notificationDTO) {
        AdminNotification notification = convertToEntity(notificationDTO);
        notification.setStatus(AdminNotification.NotificationStatus.SCHEDULED);
        notification.setCreatedBy("admin");

        AdminNotification savedNotification = adminNotificationRepository.save(notification);
        return convertToDTO(savedNotification);
    }

    // Get notifications by status
    public List<AdminNotificationDTO> getNotificationsByStatus(AdminNotification.NotificationStatus status) {
        List<AdminNotification> notifications = adminNotificationRepository.findByStatusAndIsActiveTrueOrderByCreatedDesc(status);
        return notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Search notifications
    public List<AdminNotificationDTO> searchNotifications(String keyword) {
        List<AdminNotification> notifications = adminNotificationRepository.searchByKeyword(keyword);
        return notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Clear all notifications (soft delete)
    public void clearAllNotifications() {
        adminNotificationRepository.softDeleteAll();
    }

    // Get notification statistics
    public NotificationStatistics getNotificationStatistics() {
        Long totalNotifications = (long) adminNotificationRepository.findByIsActiveTrueOrderByCreatedDesc().size();
        Long sentCount = (long) adminNotificationRepository.findByStatusAndIsActiveTrueOrderByCreatedDesc(AdminNotification.NotificationStatus.SENT).size();
        Long draftCount = (long) adminNotificationRepository.findByStatusAndIsActiveTrueOrderByCreatedDesc(AdminNotification.NotificationStatus.DRAFT).size();
        Long scheduledCount = (long) adminNotificationRepository.findByStatusAndIsActiveTrueOrderByCreatedDesc(AdminNotification.NotificationStatus.SCHEDULED).size();

        return new NotificationStatistics(totalNotifications, sentCount, draftCount, scheduledCount);
    }

    // Sync notification to user tables
    private void syncNotificationToUsers(AdminNotification adminNotification) {
        try {
            List<Long> userIds = getUserIdsByTarget(adminNotification.getTarget());

            for (Long userId : userIds) {
                Optional<UserNotification> existingNotification =
                        userNotificationRepository.findByAdminNotificationIdAndUserId(adminNotification.getId(), userId);

                if (existingNotification.isEmpty()) {
                    UserNotification userNotification = new UserNotification();
                    userNotification.setUserId(userId);
                    userNotification.setAdminNotificationId(adminNotification.getId());
                    userNotification.setTitle(adminNotification.getTitle());
                    userNotification.setMessage(adminNotification.getMessage());
                    userNotification.setType(convertToUserNotificationType(adminNotification.getType()));
                    userNotification.setPriority(convertToUserNotificationPriority(adminNotification.getPriority()));
                    userNotification.setSentBy(adminNotification.getCreatedBy());
                    userNotification.setExpiryDate(adminNotification.getExpiryDate());

                    userNotificationRepository.save(userNotification);
                }
            }

            System.out.println("✅ Synced notification to " + userIds.size() + " users");

        } catch (Exception e) {
            System.err.println("❌ Error syncing notification to users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Get user IDs based on target audience
    private List<Long> getUserIdsByTarget(AdminNotification.TargetAudience target) {
        switch (target) {
            case ALL:
                return Arrays.asList(1L, 2L, 3L, 4L, 5L);
            case ACTIVE:
                return Arrays.asList(1L, 2L, 3L);
            case INACTIVE:
                return Arrays.asList(4L, 5L);
            case PREMIUM:
                return Arrays.asList(1L, 3L);
            case NEW:
                return Arrays.asList(5L);
            default:
                return Arrays.asList(1L);
        }
    }

    // Convert admin notification type to user notification type
    private UserNotification.NotificationType convertToUserNotificationType(AdminNotification.NotificationType adminType) {
        switch (adminType) {
            case GENERAL: return UserNotification.NotificationType.GENERAL;
            case UPDATE: return UserNotification.NotificationType.UPDATE;
            case PROMOTION: return UserNotification.NotificationType.PROMOTION;
            case MAINTENANCE: return UserNotification.NotificationType.MAINTENANCE;
            case SECURITY: return UserNotification.NotificationType.SECURITY;
            default: return UserNotification.NotificationType.GENERAL;
        }
    }

    // Convert admin notification priority to user notification priority
    private UserNotification.PriorityLevel convertToUserNotificationPriority(AdminNotification.PriorityLevel adminPriority) {
        switch (adminPriority) {
            case LOW: return UserNotification.PriorityLevel.LOW;
            case MEDIUM: return UserNotification.PriorityLevel.MEDIUM;
            case HIGH: return UserNotification.PriorityLevel.HIGH;
            case URGENT: return UserNotification.PriorityLevel.URGENT;
            default: return UserNotification.PriorityLevel.MEDIUM;
        }
    }

    // Convert Entity to DTO
    private AdminNotificationDTO convertToDTO(AdminNotification notification) {
        AdminNotificationDTO dto = new AdminNotificationDTO();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        dto.setPriority(notification.getPriority());
        dto.setTarget(notification.getTarget());
        dto.setStatus(notification.getStatus());
        dto.setCreated(notification.getCreated());
        dto.setScheduleDate(notification.getScheduleDate());
        dto.setExpiryDate(notification.getExpiryDate());
        dto.setSentCount(notification.getSentCount());
        dto.setCreatedBy(notification.getCreatedBy());
        dto.setUpdatedDate(notification.getUpdatedDate());
        dto.setIsActive(notification.getIsActive());
        return dto;
    }

    // Convert DTO to Entity
    private AdminNotification convertToEntity(AdminNotificationDTO dto) {
        AdminNotification notification = new AdminNotification();
        if (dto.getId() != null) {
            notification.setId(dto.getId());
        }
        notification.setTitle(dto.getTitle());
        notification.setMessage(dto.getMessage());
        notification.setType(dto.getType());
        notification.setPriority(dto.getPriority());
        notification.setTarget(dto.getTarget());
        notification.setScheduleDate(dto.getScheduleDate());
        notification.setExpiryDate(dto.getExpiryDate());
        return notification;
    }

    // Simulate notification sending
    private int simulateNotificationSending(AdminNotification.TargetAudience target) {
        int baseCount = switch (target) {
            case ALL -> 1000 + random.nextInt(500);
            case ACTIVE -> 700 + random.nextInt(300);
            case INACTIVE -> 200 + random.nextInt(100);
            case PREMIUM -> 150 + random.nextInt(100);
            case NEW -> 50 + random.nextInt(50);
        };
        return baseCount;
    }

    // Inner class for statistics
    public static class NotificationStatistics {
        private Long total;
        private Long sent;
        private Long draft;
        private Long scheduled;

        public NotificationStatistics(Long total, Long sent, Long draft, Long scheduled) {
            this.total = total;
            this.sent = sent;
            this.draft = draft;
            this.scheduled = scheduled;
        }

        // Getters
        public Long getTotal() { return total; }
        public Long getSent() { return sent; }
        public Long getDraft() { return draft; }
        public Long getScheduled() { return scheduled; }
    }
}

package com.careertrack.modules.notification.service;

import com.careertrack.modules.notification.dto.NotificationDto;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    List<NotificationDto> getNotificationsForUser(UUID recipientId);
    void markAsRead(UUID notificationId);
}

package com.careertrack.modules.notification.dto;

import com.careertrack.modules.notification.entity.NotificationChannel;
import com.careertrack.modules.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private UUID id;
    private NotificationType notificationType;
    private NotificationChannel channel;
    private String title;
    private String message;
    private UUID recipientId;
    private String recipientEmail;
    private boolean read;
    private Instant sentAt;
}

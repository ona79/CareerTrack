package com.careertrack.modules.notification.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.notification.dto.NotificationDto;
import com.careertrack.modules.notification.entity.Notification;
import com.careertrack.modules.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public List<NotificationDto> getNotificationsForUser(UUID recipientId) {
        return notificationRepository.findByRecipientIdOrderBySentAtDesc(recipientId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable : " + notificationId));
        notif.setRead(true);
        notif.setReadAt(Instant.now());
        notificationRepository.save(notif);
    }

    private NotificationDto mapToDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .notificationType(n.getNotificationType())
                .channel(n.getChannel())
                .title(n.getTitle())
                .message(n.getMessage())
                .recipientId(n.getRecipient() != null ? n.getRecipient().getId() : null)
                .read(n.isRead())
                .sentAt(n.getSentAt())
                .build();
    }
}

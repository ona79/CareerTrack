package com.careertrack.modules.notification.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.notification.dto.NotificationDto;
import com.careertrack.modules.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Alertes in-app et notifications par courrier électronique")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lister les notifications de l'utilisateur connecté", description = "Accessible à l'utilisateur authentifié")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getMyNotifications(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.careertrack.common.security.UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationsForUser(principal.getId())));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marquer une notification comme lue", description = "Accessible à l'utilisateur connecté")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marquée comme lue", null));
    }
}

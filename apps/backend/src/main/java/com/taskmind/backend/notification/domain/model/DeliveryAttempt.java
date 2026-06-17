package com.taskmind.backend.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAttempt(
        UUID id,
        UUID notificationId,
        UUID userId,
        NotificationChannel channel,
        DeliveryStatus status,
        String errorMessage,
        Instant attemptedAt) {}

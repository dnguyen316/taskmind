package com.taskmind.backend.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public record NotificationPreferenceProcess(
        UUID userId, NotificationChannel channel, boolean enabled, Instant processedAt) {}

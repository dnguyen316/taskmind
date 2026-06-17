package com.taskmind.backend.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ReminderState(UUID taskId, UUID userId, Instant remindedAt) {}

package com.taskmind.backend.notification.domain.repository;

import com.taskmind.backend.notification.domain.model.*;
import java.time.Instant;
import java.util.*;

public interface NotificationRepository {
    Notification save(Notification n);

    Optional<Notification> findById(UUID id);

    List<Notification> findByRecipient(UUID userId, int page, int size);

    long unreadCount(UUID userId);

    int markAllRead(UUID userId, Instant now);

    List<Notification> unreadOlderThan(UUID userId, Instant before);

    void recordDelivery(DeliveryAttempt attempt);

    boolean reminderExists(UUID taskId, UUID userId);

    void recordReminder(ReminderState state);

    List<ReminderCandidate> dueReminderCandidates(Instant now, int limit);

    record ReminderCandidate(
            UUID taskId, UUID userId, UUID assigneeId, String title, Instant dueAt) {}
}

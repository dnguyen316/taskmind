package com.taskmind.backend.notification.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.notification.domain.model.*;
import com.taskmind.backend.notification.domain.repository.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationApplicationService {
    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository preferences;

    public NotificationApplicationService(
            NotificationRepository n, NotificationPreferenceRepository p) {
        notifications = n;
        preferences = p;
    }

    public List<Notification> list(AuthenticatedUser user, int page, int size) {
        return notifications.findByRecipient(user.userId(), page, size);
    }

    public long unreadCount(AuthenticatedUser user) {
        return notifications.unreadCount(user.userId());
    }

    @Transactional
    public Optional<Notification> markRead(AuthenticatedUser user, UUID id) {
        return notifications
                .findById(id)
                .filter(n -> n.recipientUserId().equals(user.userId()))
                .map(n -> notifications.save(n.markRead(Instant.now())));
    }

    @Transactional
    public int markAllRead(AuthenticatedUser user) {
        return notifications.markAllRead(user.userId(), Instant.now());
    }

    public NotificationPreference preferences(AuthenticatedUser user) {
        return preferences
                .findByUserId(user.userId())
                .orElseGet(
                        () ->
                                preferences.save(
                                        NotificationPreference.defaults(
                                                user.userId(), Instant.now())));
    }

    @Transactional
    public NotificationPreference updatePreferences(
            AuthenticatedUser user,
            Long expectedVersion,
            boolean inApp,
            boolean email,
            boolean slack,
            String webhook,
            String channel) {
        Instant now = Instant.now();
        NotificationPreference existing = preferences(user);
        Long version = expectedVersion != null ? expectedVersion : existing.version();
        return preferences.save(
                new NotificationPreference(
                        user.userId(),
                        version,
                        inApp,
                        email,
                        slack,
                        webhook,
                        channel,
                        existing.createdAt(),
                        now));
    }
}

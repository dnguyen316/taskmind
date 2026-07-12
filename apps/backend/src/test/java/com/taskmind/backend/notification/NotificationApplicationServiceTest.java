package com.taskmind.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.notification.application.*;
import com.taskmind.backend.notification.domain.model.*;
import com.taskmind.backend.notification.domain.repository.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class NotificationApplicationServiceTest {
    @Test
    void scopesReadStateAndPreferencesToAuthenticatedUser() {
        InMemoryNotifications repo = new InMemoryNotifications();
        InMemoryPrefs prefs = new InMemoryPrefs();
        NotificationApplicationService app = new NotificationApplicationService(repo, prefs);
        NotificationService svc =
                new NotificationService(repo, prefs, new NotificationSseHub(), new NotificationDeliveryCoordinator(repo, java.time.Duration.ZERO));
        UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID();
        Notification n1 =
                svc.notify(
                        u1,
                        NotificationType.TASK_ASSIGNED,
                        "Assigned",
                        "Body",
                        UUID.randomUUID(),
                        "/tasks/1");
        svc.notify(
                u2, NotificationType.TASK_ASSIGNED, "Other", "Body", UUID.randomUUID(), "/tasks/2");
        assertThat(app.list(user(u1), 0, 10)).extracting(Notification::id).containsExactly(n1.id());
        assertThat(app.markRead(user(u2), n1.id())).isEmpty();
        assertThat(app.unreadCount(user(u1))).isEqualTo(1);
        assertThat(app.markRead(user(u1), n1.id())).isPresent();
        assertThat(app.unreadCount(user(u1))).isZero();
        var updated =
                app.updatePreferences(user(u1), null, false, true, true, "https://hooks.example", "#ops");
        assertThat(updated.inAppEnabled()).isFalse();
        assertThat(updated.slackEnabled()).isTrue();
    }

    static AuthenticatedUser user(UUID id) {
        return new AuthenticatedUser(id, Set.of("USER"));
    }

    static class InMemoryPrefs implements NotificationPreferenceRepository {
        Map<UUID, NotificationPreference> data = new HashMap<>();

        public Optional<NotificationPreference> findByUserId(UUID u) {
            return Optional.ofNullable(data.get(u));
        }

        public NotificationPreference save(NotificationPreference p) {
            data.put(p.userId(), p);
            return p;
        }
    }

    static class InMemoryNotifications implements NotificationRepository {
        Map<UUID, Notification> data = new LinkedHashMap<>();
        List<DeliveryAttempt> attempts = new ArrayList<>();
        Set<String> reminders = new HashSet<>();
        Set<UUID> claimed = new HashSet<>();

        public Notification save(Notification n) {
            data.put(n.id(), n);
            return n;
        }

        public Optional<Notification> findById(UUID id) {
            return Optional.ofNullable(data.get(id));
        }

        public List<Notification> findByRecipient(UUID u, int p, int s) {
            return data.values().stream().filter(n -> n.recipientUserId().equals(u)).toList();
        }

        public long unreadCount(UUID u) {
            return data.values().stream()
                    .filter(n -> n.recipientUserId().equals(u) && !n.read())
                    .count();
        }

        public int markAllRead(UUID u, Instant now) {
            int c = 0;
            for (var n : new ArrayList<>(data.values()))
                if (n.recipientUserId().equals(u) && !n.read()) {
                    data.put(n.id(), n.markRead(now));
                    c++;
                }
            return c;
        }

        public List<Notification> unreadOlderThan(UUID u, Instant b) {
            return findByRecipient(u, 0, 100).stream()
                    .filter(n -> !n.read() && !n.createdAt().isAfter(b))
                    .toList();
        }

        public void recordDelivery(DeliveryAttempt a) {
            attempts.add(a);
        }

        public Optional<DeliveryAttempt> latestDeliveryAttempt(UUID notificationId, NotificationChannel channel) {
            return attempts.stream()
                    .filter(a -> a.notificationId().equals(notificationId) && a.channel() == channel)
                    .reduce((first, second) -> second);
        }

        public List<DeliveryAttempt> claimPendingDeliveries(Instant now, int limit) {
            List<DeliveryAttempt> claimed =
                    attempts.stream()
                            .filter(
                                    a ->
                                            a.status() == DeliveryStatus.PENDING
                                                    && !a.attemptedAt().isAfter(now)
                                                    && !this.claimed.contains(a.id()))
                            .limit(limit)
                            .toList();
            claimed.forEach(a -> this.claimed.add(a.id()));
            return claimed;
        }

        public void markDeliverySent(UUID deliveryAttemptId, Instant now) {
            updateDelivery(deliveryAttemptId, DeliveryStatus.SENT, null, now);
        }

        public void markDeliveryFailed(UUID deliveryAttemptId, String errorMessage, Instant retryAt) {
            updateDelivery(deliveryAttemptId, DeliveryStatus.FAILED, errorMessage, retryAt);
        }

        private void updateDelivery(UUID deliveryAttemptId, DeliveryStatus status, String error, Instant at) {
            for (int i = 0; i < attempts.size(); i++) {
                DeliveryAttempt current = attempts.get(i);
                if (current.id().equals(deliveryAttemptId)) {
                    attempts.set(
                            i,
                            new DeliveryAttempt(
                                    current.id(),
                                    current.notificationId(),
                                    current.userId(),
                                    current.channel(),
                                    status,
                                    error,
                                    at));
                    return;
                }
            }
        }

        public boolean reminderExists(UUID t, UUID u) {
            return reminders.contains(t + ":" + u);
        }

        public void recordReminder(ReminderState s) {
            reminders.add(s.taskId() + ":" + s.userId());
        }

        public List<ReminderCandidate> dueReminderCandidates(Instant n, int l) {
            return List.of();
        }
    }
}

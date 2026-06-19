package com.taskmind.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskmind.backend.notification.NotificationApplicationServiceTest.*;
import com.taskmind.backend.notification.application.*;
import com.taskmind.backend.notification.domain.model.*;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationProviderFailureTest {
    @Test
    void persistsInAppAndRecordsSlackFailure() {
        var repo = new InMemoryNotifications();
        var prefs = new InMemoryPrefs();
        UUID user = UUID.randomUUID();
        prefs.save(
                new NotificationPreference(
                        user, null, true, true, true, "hook", "#c", Instant.now(), Instant.now()));
        var service =
                new NotificationService(
                        repo,
                        prefs,
                        new NotificationSseHub(),
                        (n, p) -> {
                            throw new IllegalStateException("boom");
                        },
                        new NotificationDeliveryCoordinator(repo, java.time.Duration.ZERO));
        var n =
                service.notify(
                        user,
                        NotificationType.TASK_COMMENT,
                        "Comment",
                        "New comment",
                        UUID.randomUUID(),
                        "/tasks/t");
        assertThat(n).isNotNull();
        assertThat(repo.attempts).hasSize(1);
        assertThat(repo.attempts.get(0).status()).isEqualTo(DeliveryStatus.FAILED);
    }

    @Test
    void backsOffFailedDeliveryUntilRetryWindowPasses() {
        var repo = new InMemoryNotifications();
        UUID user = UUID.randomUUID();
        Notification notification =
                new Notification(
                        UUID.randomUUID(),
                        null,
                        user,
                        NotificationType.TASK_COMMENT,
                        "Comment",
                        "New comment",
                        UUID.randomUUID(),
                        "/tasks/t",
                        false,
                        null,
                        Instant.now());
        repo.recordDelivery(
                new DeliveryAttempt(
                        UUID.randomUUID(),
                        notification.id(),
                        user,
                        NotificationChannel.SLACK,
                        DeliveryStatus.FAILED,
                        "boom",
                        Instant.parse("2026-06-19T10:00:00Z")));
        var delivery = new NotificationDeliveryCoordinator(repo, Duration.ofMinutes(15));

        assertThat(
                        delivery.shouldAttempt(
                                notification,
                                NotificationChannel.SLACK,
                                Instant.parse("2026-06-19T10:05:00Z")))
                .isFalse();
        assertThat(
                        delivery.shouldAttempt(
                                notification,
                                NotificationChannel.SLACK,
                                Instant.parse("2026-06-19T10:16:00Z")))
                .isTrue();
    }

}

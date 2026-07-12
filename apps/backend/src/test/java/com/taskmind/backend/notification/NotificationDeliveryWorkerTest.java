package com.taskmind.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskmind.backend.notification.NotificationApplicationServiceTest.*;
import com.taskmind.backend.notification.application.*;
import com.taskmind.backend.notification.domain.model.*;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class NotificationDeliveryWorkerTest {
    @Test
    void sendsQueuedSlackAndRecordsSuccess() {
        Fixture fx = fixture();
        AtomicInteger sends = new AtomicInteger();
        var worker = worker(fx, (n, p) -> sends.incrementAndGet());

        assertThat(worker.processBatch(Instant.parse("2026-06-19T10:00:00Z"), 10)).isEqualTo(1);

        assertThat(sends).hasValue(1);
        assertThat(fx.repo.attempts).extracting(DeliveryAttempt::status).containsExactly(DeliveryStatus.SENT);
    }

    @Test
    void recordsFailureWithRetryBackoff() {
        Fixture fx = fixture();
        var worker = worker(fx, (n, p) -> { throw new IllegalStateException("boom"); });

        worker.processBatch(Instant.parse("2026-06-19T10:00:00Z"), 10);

        assertThat(fx.repo.attempts).hasSize(1);
        DeliveryAttempt failed = fx.repo.attempts.get(0);
        assertThat(failed.status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(failed.errorMessage()).isEqualTo("boom");
        assertThat(failed.attemptedAt()).isEqualTo(Instant.parse("2026-06-19T10:15:00Z"));
    }

    @Test
    void skipsPendingUntilBackoffExpiresAfterRequeue() {
        Fixture fx = fixture();
        AtomicInteger sends = new AtomicInteger();
        var worker = worker(fx, (n, p) -> sends.incrementAndGet());
        fx.repo.attempts.clear();
        fx.repo.recordDelivery(attempt(fx.notification, DeliveryStatus.PENDING, Instant.parse("2026-06-19T10:15:00Z")));

        assertThat(worker.processBatch(Instant.parse("2026-06-19T10:05:00Z"), 10)).isZero();
        assertThat(worker.processBatch(Instant.parse("2026-06-19T10:16:00Z"), 10)).isEqualTo(1);

        assertThat(sends).hasValue(1);
    }

    @Test
    void duplicateWorkersDoNotSendSameClaimTwice() {
        Fixture fx = fixture();
        AtomicInteger sends = new AtomicInteger();
        var worker1 = worker(fx, (n, p) -> sends.incrementAndGet());
        var worker2 = worker(fx, (n, p) -> sends.incrementAndGet());

        assertThat(worker1.processBatch(Instant.parse("2026-06-19T10:00:00Z"), 10)).isEqualTo(1);
        assertThat(worker2.processBatch(Instant.parse("2026-06-19T10:00:00Z"), 10)).isZero();

        assertThat(sends).hasValue(1);
    }

    private static NotificationDeliveryWorker worker(Fixture fx, com.taskmind.backend.common.SlackNotifier slack) {
        return new NotificationDeliveryWorker(
                fx.repo,
                fx.prefs,
                slack,
                (preference, notifications) -> {},
                new NotificationDeliveryCoordinator(fx.repo, Duration.ofMinutes(15)),
                10);
    }

    private static Fixture fixture() {
        var repo = new InMemoryNotifications();
        var prefs = new InMemoryPrefs();
        UUID user = UUID.randomUUID();
        prefs.save(new NotificationPreference(user, null, true, true, true, "hook", "#c", Instant.now(), Instant.now()));
        Notification notification =
                new Notification(UUID.randomUUID(), null, user, NotificationType.TASK_COMMENT, "Comment", "Body", UUID.randomUUID(), "/tasks/1", false, null, Instant.now());
        repo.save(notification);
        repo.recordDelivery(attempt(notification, DeliveryStatus.PENDING, Instant.parse("2026-06-19T10:00:00Z")));
        return new Fixture(repo, prefs, notification);
    }

    private static DeliveryAttempt attempt(Notification notification, DeliveryStatus status, Instant at) {
        return new DeliveryAttempt(UUID.randomUUID(), notification.id(), notification.recipientUserId(), NotificationChannel.SLACK, status, null, at);
    }

    private record Fixture(InMemoryNotifications repo, InMemoryPrefs prefs, Notification notification) {}
}

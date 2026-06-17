package com.taskmind.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskmind.backend.notification.NotificationApplicationServiceTest.*;
import com.taskmind.backend.notification.application.*;
import com.taskmind.backend.notification.domain.model.*;
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
                        });
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
}

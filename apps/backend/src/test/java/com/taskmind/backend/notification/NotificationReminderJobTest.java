package com.taskmind.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskmind.backend.notification.NotificationApplicationServiceTest.*;
import com.taskmind.backend.notification.application.*;
import com.taskmind.backend.notification.domain.repository.NotificationRepository.ReminderCandidate;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class NotificationReminderJobTest {
    @Test
    void createsOneReminderForEligibleTask() {
        var repo =
                new InMemoryNotifications() {
                    UUID task = UUID.randomUUID(), owner = UUID.randomUUID();

                    public List<ReminderCandidate> dueReminderCandidates(Instant n, int l) {
                        return List.of(
                                new ReminderCandidate(task, owner, null, "Due", n.minusSeconds(1)));
                    }
                };
        var prefs = new InMemoryPrefs();
        var service = new NotificationService(repo, prefs, new NotificationSseHub(), new NotificationDeliveryCoordinator(repo, java.time.Duration.ZERO));
        new NotificationReminderJob(repo, service, true).run();
        new NotificationReminderJob(repo, service, true).run();
        assertThat(repo.data).hasSize(1);
    }
}

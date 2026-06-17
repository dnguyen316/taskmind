package com.taskmind.backend.notification.application;

import com.taskmind.backend.notification.domain.model.*;
import com.taskmind.backend.notification.domain.repository.NotificationRepository;
import java.time.Instant;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationReminderJob {
    private final NotificationRepository repository;
    private final NotificationService service;
    private final boolean enabled;

    public NotificationReminderJob(
            NotificationRepository r,
            NotificationService s,
            @Value("${taskmind.notifications.enabled:true}") boolean e) {
        repository = r;
        service = s;
        enabled = e;
    }

    @Scheduled(fixedDelayString = "${taskmind.notifications.reminders.fixed-delay:60000}")
    @SchedulerLock(
            name = "notificationReminderJob",
            lockAtMostFor = "PT5M",
            lockAtLeastFor = "PT5S")
    public void run() {
        if (!enabled) return;
        for (var c : repository.dueReminderCandidates(Instant.now(), 100)) {
            UUID recipient = c.assigneeId() != null ? c.assigneeId() : c.userId();
            if (repository.reminderExists(c.taskId(), recipient)) continue;
            service.notify(
                    recipient,
                    NotificationType.TASK_DUE_SOON,
                    "Task due soon",
                    c.title(),
                    c.taskId(),
                    "/tasks/" + c.taskId());
            repository.recordReminder(new ReminderState(c.taskId(), recipient, Instant.now()));
        }
    }
}

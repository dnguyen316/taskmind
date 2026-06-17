package com.taskmind.backend.notification.infrastructure.slack;

import com.taskmind.backend.common.SlackNotifier;
import com.taskmind.backend.notification.domain.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SlackNotificationSender implements SlackNotifier {
    private final String mode;

    public SlackNotificationSender(
            @Value("${taskmind.notifications.slack.delivery:fake}") String mode) {
        this.mode = mode;
    }

    public void send(Notification notification, NotificationPreference preference) {
        if ("fail".equalsIgnoreCase(mode))
            throw new IllegalStateException("Slack delivery failed by configuration");
    }
}

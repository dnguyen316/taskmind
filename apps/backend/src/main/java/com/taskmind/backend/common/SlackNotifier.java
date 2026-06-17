package com.taskmind.backend.common;

import com.taskmind.backend.notification.domain.model.Notification;
import com.taskmind.backend.notification.domain.model.NotificationPreference;

public interface SlackNotifier {
    void send(Notification notification, NotificationPreference preference);
}

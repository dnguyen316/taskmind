package com.taskmind.backend.common;

import com.taskmind.backend.notification.domain.model.Notification;
import com.taskmind.backend.notification.domain.model.NotificationPreference;
import java.util.List;

public interface EmailSender {
    void sendDigest(NotificationPreference preference, List<Notification> notifications);
}

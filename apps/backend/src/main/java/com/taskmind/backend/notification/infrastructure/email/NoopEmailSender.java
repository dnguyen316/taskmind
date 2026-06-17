package com.taskmind.backend.notification.infrastructure.email;

import com.taskmind.backend.common.EmailSender;
import com.taskmind.backend.notification.domain.model.*;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NoopEmailSender implements EmailSender {
    public void sendDigest(NotificationPreference preference, List<Notification> notifications) {}
}

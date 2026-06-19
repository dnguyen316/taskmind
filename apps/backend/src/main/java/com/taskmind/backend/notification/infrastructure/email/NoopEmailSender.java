package com.taskmind.backend.notification.infrastructure.email;

import com.taskmind.backend.common.EmailSender;
import com.taskmind.backend.notification.domain.model.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class NoopEmailSender implements EmailSender {
    private final JavaMailSender mailSender;
    private final String delivery;
    private final String from;
    private final String defaultRecipient;

    public NoopEmailSender(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${taskmind.notifications.email.delivery:smtp}") String delivery,
            @Value("${taskmind.notifications.email.from:notifications@taskmind.local}") String from,
            @Value("${taskmind.notifications.email.default-recipient:}") String defaultRecipient) {
        this.mailSender = mailSender.getIfAvailable();
        this.delivery = delivery;
        this.from = from;
        this.defaultRecipient = defaultRecipient;
    }

    @Override
    public void sendDigest(NotificationPreference preference, List<Notification> notifications) {
        if ("fake".equalsIgnoreCase(delivery)) {
            return;
        }
        if ("disabled".equalsIgnoreCase(delivery)) {
            throw new IllegalStateException("Email delivery is disabled");
        }
        if (notifications.isEmpty()) {
            return;
        }
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender is required for SMTP email delivery");
        }
        String recipient = resolveRecipient(preference);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("TaskMind notification digest (" + notifications.size() + ")");
        message.setText(body(notifications));
        mailSender.send(message);
    }

    private String resolveRecipient(NotificationPreference preference) {
        if (defaultRecipient != null && !defaultRecipient.isBlank()) {
            return defaultRecipient;
        }
        throw new IllegalStateException(
                "No email recipient configured for notification digest user " + preference.userId());
    }

    private static String body(List<Notification> notifications) {
        StringBuilder body = new StringBuilder("You have unread TaskMind notifications:\n\n");
        for (Notification notification : notifications) {
            body.append("- ")
                    .append(notification.title())
                    .append(": ")
                    .append(notification.body() == null ? "" : notification.body())
                    .append("\n")
                    .append(notification.actionUrl() == null ? "" : notification.actionUrl())
                    .append("\n\n");
        }
        return body.toString();
    }
}

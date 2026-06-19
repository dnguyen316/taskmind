package com.taskmind.backend.notification.infrastructure.slack;

import com.taskmind.backend.common.SlackNotifier;
import com.taskmind.backend.notification.domain.model.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SlackNotificationSender implements SlackNotifier {
    private final RestClient restClient;
    private final String delivery;
    private final String botToken;

    public SlackNotificationSender(
            RestClient.Builder restClientBuilder,
            @Value("${taskmind.notifications.slack.delivery:webhook}") String delivery,
            @Value("${taskmind.notifications.slack.bot-token:}") String botToken) {
        this.restClient = restClientBuilder.build();
        this.delivery = delivery;
        this.botToken = botToken;
    }

    @Override
    public void send(Notification notification, NotificationPreference preference) {
        if ("fake".equalsIgnoreCase(delivery)) {
            return;
        }
        if ("fail".equalsIgnoreCase(delivery)) {
            throw new IllegalStateException("Slack delivery failed by configuration");
        }
        if ("api".equalsIgnoreCase(delivery)) {
            sendWithApi(notification, preference);
            return;
        }
        sendWithWebhook(notification, preference);
    }

    private void sendWithWebhook(Notification notification, NotificationPreference preference) {
        if (preference.slackWebhookUrl() == null || preference.slackWebhookUrl().isBlank()) {
            throw new IllegalStateException("Slack webhook URL is required for webhook delivery");
        }
        restClient
                .post()
                .uri(preference.slackWebhookUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("text", text(notification)))
                .retrieve()
                .toBodilessEntity();
    }

    private void sendWithApi(Notification notification, NotificationPreference preference) {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("Slack bot token is required for API delivery");
        }
        if (preference.slackChannel() == null || preference.slackChannel().isBlank()) {
            throw new IllegalStateException("Slack channel is required for API delivery");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("channel", preference.slackChannel());
        payload.put("text", text(notification));
        restClient
                .post()
                .uri("https://slack.com/api/chat.postMessage")
                .headers(headers -> headers.setBearerAuth(botToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    private static String text(Notification notification) {
        String body = notification.body() == null ? "" : " - " + notification.body();
        String action = notification.actionUrl() == null ? "" : "\n" + notification.actionUrl();
        return "TaskMind: " + notification.title() + body + action;
    }
}

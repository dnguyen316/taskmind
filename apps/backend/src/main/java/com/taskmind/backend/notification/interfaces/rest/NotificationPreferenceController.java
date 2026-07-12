package com.taskmind.backend.notification.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.notification.application.NotificationApplicationService;
import com.taskmind.backend.notification.domain.model.NotificationPreference;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/notifications/preferences")
public class NotificationPreferenceController {
    private final NotificationApplicationService service;

    public NotificationPreferenceController(NotificationApplicationService s) {
        service = s;
    }

    @GetMapping
    public NotificationPreference get(AuthenticatedUser u) {
        return service.preferences(u);
    }

    @PutMapping
    public NotificationPreference put(
            AuthenticatedUser u, @RequestBody UpdateNotificationPreferencesRequest r) {
        return service.updatePreferences(
                u,
                r.version(),
                r.inAppEnabled(),
                r.emailDigestEnabled(),
                r.slackEnabled(),
                r.slackWebhookUrl(),
                r.slackChannel());
    }

    public record UpdateNotificationPreferencesRequest(
            Long version,
            boolean inAppEnabled,
            boolean emailDigestEnabled,
            boolean slackEnabled,
            String slackWebhookUrl,
            String slackChannel) {}
}

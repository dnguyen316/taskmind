package com.taskmind.backend.notification.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.notification.application.NotificationApplicationService;
import com.taskmind.backend.notification.domain.model.NotificationPreference;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/notifications/preferences")
public class NotificationPreferenceController {
    private final NotificationApplicationService service;

    public NotificationPreferenceController(NotificationApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public NotificationPreference get(AuthenticatedUser authenticatedUser) {
        return service.preferences(authenticatedUser);
    }

    @PutMapping
    public NotificationPreference put(
            AuthenticatedUser authenticatedUser, @RequestBody UpdateNotificationPreferencesRequest request) {
        return service.updatePreferences(
                authenticatedUser,
                request.version(),
                request.inAppEnabled(),
                request.emailDigestEnabled(),
                request.slackEnabled(),
                request.slackWebhookUrl(),
                request.slackChannel());
    }

    public record UpdateNotificationPreferencesRequest(
            Long version,
            boolean inAppEnabled,
            boolean emailDigestEnabled,
            boolean slackEnabled,
            String slackWebhookUrl,
            String slackChannel) {}
}

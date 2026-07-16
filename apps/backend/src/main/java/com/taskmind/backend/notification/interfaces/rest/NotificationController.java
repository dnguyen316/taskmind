package com.taskmind.backend.notification.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.notification.application.*;
import com.taskmind.backend.notification.domain.model.Notification;
import jakarta.validation.constraints.Min;
import java.util.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1/notifications")
@Validated
public class NotificationController {
    private final NotificationApplicationService service;
    private final NotificationSseHub hub;

    public NotificationController(NotificationApplicationService service, NotificationSseHub sseHub) {
        this.service = service;
        this.hub = sseHub;
    }

    @GetMapping
    public List<Notification> list(
            AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return service.list(authenticatedUser, page, size);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unread(AuthenticatedUser authenticatedUser) {
        return new UnreadCountResponse(service.unreadCount(authenticatedUser));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> read(AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
        return service.markRead(authenticatedUser, id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/read-all")
    public MarkAllReadResponse readAll(AuthenticatedUser authenticatedUser) {
        return new MarkAllReadResponse(
                service.markAllRead(authenticatedUser), service.unreadCount(authenticatedUser));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(AuthenticatedUser authenticatedUser) {
        return hub.subscribe(authenticatedUser.userId());
    }

    public record UnreadCountResponse(long unreadCount) {}

    public record MarkAllReadResponse(int updatedCount, long unreadCount) {}
}

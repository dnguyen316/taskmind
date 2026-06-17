package com.taskmind.backend.notification.application;

import com.taskmind.backend.notification.domain.model.Notification;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class NotificationSseHub {
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(30).toMillis());
        emitters.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup =
                () -> emitters.getOrDefault(userId, new CopyOnWriteArrayList<>()).remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
        } catch (IOException ignored) {
            cleanup.run();
        }
        return emitter;
    }

    public void publish(Notification notification) {
        List<SseEmitter> userEmitters = emitters.get(notification.recipientUserId());
        if (userEmitters == null) return;
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("notification")
                                .id(notification.id().toString())
                                .data(notification));
            } catch (IOException | IllegalStateException ex) {
                userEmitters.remove(emitter);
            }
        }
    }

    public int subscriberCount(UUID userId) {
        return emitters.getOrDefault(userId, new CopyOnWriteArrayList<>()).size();
    }
}

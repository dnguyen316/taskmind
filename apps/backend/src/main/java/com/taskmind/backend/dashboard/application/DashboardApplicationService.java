package com.taskmind.backend.dashboard.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.dashboard.infrastructure.DashboardCacheService;
import com.taskmind.backend.relay.RelayContextPort;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class DashboardApplicationService {
    private final RelayContextPort relay;
    private final DashboardCacheService cache;

    public DashboardApplicationService(RelayContextPort relay, DashboardCacheService cache) {
        this.relay = relay;
        this.cache = cache;
    }

    public DashboardResponse dashboard(AuthenticatedUser requester) {
        return cache.get(requester.userId())
                .orElseGet(() -> cache.put(requester.userId(), aggregate(requester)));
    }

    private DashboardResponse aggregate(AuthenticatedUser requester) {
        List<MyTaskItem> tasks =
                relay.userTasks(requester.userId()).stream().map(this::task).toList();
        int completed =
                (int) tasks.stream().filter(t -> "DONE".equalsIgnoreCase(t.status())).count();
        int open = tasks.size() - completed;
        List<ActivitySnippet> activity =
                relay
                        .projectMetrics(
                                tasks.stream()
                                        .map(MyTaskItem::projectId)
                                        .filter(Objects::nonNull)
                                        .findFirst()
                                        .orElse(new UUID(0L, 0L)))
                        .stream()
                        .map(this::activity)
                        .toList();
        int events = activity.stream().mapToInt(ActivitySnippet::eventsIngested).sum();
        return new DashboardResponse(
                new KpiMetrics(
                        open,
                        completed,
                        events,
                        tasks.isEmpty() ? 0.0 : (double) completed / tasks.size()),
                tasks,
                activity);
    }

    private MyTaskItem task(Map<String, Object> m) {
        return new MyTaskItem(
                (UUID) m.get("task_id"),
                (UUID) m.get("project_id"),
                String.valueOf(m.getOrDefault("title", "Untitled")),
                String.valueOf(m.getOrDefault("status", "TODO")),
                offset(m.get("updated_at")));
    }

    private ActivitySnippet activity(Map<String, Object> m) {
        Object d = m.get("metric_date");
        LocalDate date =
                d instanceof java.sql.Date sql
                        ? sql.toLocalDate()
                        : LocalDate.parse(String.valueOf(d));
        return new ActivitySnippet(
                date,
                num(m, "tasks_created"),
                num(m, "tasks_completed"),
                num(m, "events_ingested"));
    }

    private int num(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.intValue() : 0;
    }

    private OffsetDateTime offset(Object value) {
        if (value instanceof OffsetDateTime o) return o;
        if (value instanceof Timestamp t) return t.toInstant().atOffset(ZoneOffset.UTC);
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}

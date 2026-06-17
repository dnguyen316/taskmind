package com.taskmind.backend.dashboard.infrastructure;

import com.taskmind.backend.dashboard.application.DashboardResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DashboardCacheService {
    private final boolean enabled;
    private final ConcurrentHashMap<UUID, DashboardResponse> cache = new ConcurrentHashMap<>();

    public DashboardCacheService(
            @Value("${taskmind.dashboard.cache.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public Optional<DashboardResponse> get(UUID userId) {
        return enabled ? Optional.ofNullable(cache.get(userId)) : Optional.empty();
    }

    public DashboardResponse put(UUID userId, DashboardResponse response) {
        if (enabled) cache.put(userId, response);
        return response;
    }

    public void evict(UUID userId) {
        cache.remove(userId);
    }
}

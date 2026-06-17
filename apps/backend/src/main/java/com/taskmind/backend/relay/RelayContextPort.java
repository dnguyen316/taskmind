package com.taskmind.backend.relay;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface RelayContextPort {
    List<Map<String, Object>> userTasks(UUID userId);

    List<Map<String, Object>> projectMetrics(UUID projectId);
}

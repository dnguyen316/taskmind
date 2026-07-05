package com.taskmind.backend.task.interfaces.rest.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.task.domain.model.SavedTaskView;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SavedTaskViewResponse(UUID id, Long version, UUID userId, String name, Map<String,Object> filters, boolean builtIn, Instant createdAt, Instant updatedAt) {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static SavedTaskViewResponse from(SavedTaskView v) {
        try {
            return new SavedTaskViewResponse(v.id(), v.version(), v.userId(), v.name(), MAPPER.readValue(v.filtersJson(), new TypeReference<>() {}), v.builtIn(), v.createdAt(), v.updatedAt());
        } catch (Exception e) {
            return new SavedTaskViewResponse(v.id(), v.version(), v.userId(), v.name(), Map.of(), v.builtIn(), v.createdAt(), v.updatedAt());
        }
    }
}

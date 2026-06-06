package com.taskmind.backend.activity.infrastructure.persistence.jpa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ActivityJsonConverters {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ActivityJsonConverters() {}

    public static String toJson(JsonNode node) {
        return node == null ? "{}" : node.toString();
    }
}

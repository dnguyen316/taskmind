package com.taskmind.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class DomainEventMapper {
    private final ObjectMapper objectMapper;

    public DomainEventMapper() {
        this(new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    public DomainEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().registerModule(new JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String toJson(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to serialize domain event", ex);
        }
    }

    public DomainEvent fromJson(String json) {
        try {
            return objectMapper.readValue(json, DomainEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to deserialize domain event", ex);
        }
    }
}

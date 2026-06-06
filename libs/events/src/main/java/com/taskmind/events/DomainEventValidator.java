package com.taskmind.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

public class DomainEventValidator {
    private final ObjectMapper objectMapper;
    private final JsonSchema schema;

    public DomainEventValidator() {
        this(new ObjectMapper());
    }

    public DomainEventValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        InputStream schemaStream =
                DomainEventValidator.class.getResourceAsStream("/schema/domain-event-v1.json");
        this.schema =
                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(schemaStream);
    }

    public void validate(DomainEvent event) {
        JsonNode node = objectMapper.valueToTree(event);
        Set<String> errors = schema.validate(node).stream().map(Object::toString).collect(Collectors.toSet());
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid domain event: " + errors);
        }
        if (!EventTypeRegistry.isKnown(event.eventType())) {
            throw new IllegalArgumentException("Unknown event type: " + event.eventType());
        }
    }
}

package com.taskmind.ai.contracts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;
import java.util.Objects;

/** Stable provider-neutral Nova capability identifier. */
public record AiCapabilityId(String value) {
    public static final AiCapabilityId CHAT = new AiCapabilityId("chat");
    public static final AiCapabilityId CAPTURE = new AiCapabilityId("capture");
    public static final AiCapabilityId WEEKLY_REVIEW = new AiCapabilityId("weekly-review");

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public AiCapabilityId {
        Objects.requireNonNull(value, "value must not be null");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    @JsonValue
    public String value() {
        return value;
    }
}

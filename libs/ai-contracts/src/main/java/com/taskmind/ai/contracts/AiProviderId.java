package com.taskmind.ai.contracts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;
import java.util.Objects;

/** Provider-neutral identifier for a Nova model provider. */
public record AiProviderId(String value) {
    public static final AiProviderId MOCK = new AiProviderId("mock");
    public static final AiProviderId OPENAI = new AiProviderId("openai");
    public static final AiProviderId ANTHROPIC = new AiProviderId("anthropic");
    public static final AiProviderId NAMC = new AiProviderId("namc");

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public AiProviderId {
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

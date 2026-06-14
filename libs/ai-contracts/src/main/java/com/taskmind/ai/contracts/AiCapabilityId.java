package com.taskmind.ai.contracts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;
import java.util.Objects;

/** Stable provider-neutral Nova capability identifier. */
public record AiCapabilityId(String value) {
    public static final AiCapabilityId CHAT = new AiCapabilityId("chat");
    public static final AiCapabilityId CAPTURE = new AiCapabilityId("capture");
    public static final AiCapabilityId GOAL_BREAKDOWN = new AiCapabilityId("goal-breakdown");
    public static final AiCapabilityId WEEKLY_REVIEW = new AiCapabilityId("weekly-review");
    public static final AiCapabilityId PROJECT_BRIEF = new AiCapabilityId("project-brief");
    public static final AiCapabilityId DESCRIBE_TASK = new AiCapabilityId("describe-task");
    public static final AiCapabilityId AUTOCOMPLETE_TASK = new AiCapabilityId("autocomplete-task");
    public static final AiCapabilityId TRANSLATE_TASK = new AiCapabilityId("translate-task");
    public static final AiCapabilityId DURATION_ESTIMATE = new AiCapabilityId("duration-estimate");
    public static final AiCapabilityId RATIONALE_PHRASE = new AiCapabilityId("rationale-phrase");
    public static final AiCapabilityId DASHBOARD_INSIGHTS = new AiCapabilityId("dashboard-insights");

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public AiCapabilityId {
        Objects.requireNonNull(value, "value must not be null");
        value = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    @JsonValue
    public String value() {
        return value;
    }
}

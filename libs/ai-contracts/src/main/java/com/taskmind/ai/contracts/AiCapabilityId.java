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
    public static final AiCapabilityId ACTIVITY_SEARCH_ASSIST = new AiCapabilityId("activity-search-assist");
    public static final AiCapabilityId SPEC_OUTLINE = new AiCapabilityId("spec-outline");
    public static final AiCapabilityId SPEC_ENRICH = new AiCapabilityId("spec-enrich");
    public static final AiCapabilityId SPEC_BREAKDOWN = new AiCapabilityId("spec-breakdown");
    public static final AiCapabilityId SPEC_BREAKDOWN_SECTION = new AiCapabilityId("spec-breakdown-section");
    public static final AiCapabilityId SPEC_MERGE = new AiCapabilityId("spec-merge");
    public static final AiCapabilityId SPEC_SUGGEST_LINKS = new AiCapabilityId("spec-suggest-links");
    public static final AiCapabilityId TASK_RESOLUTION_AGENT = new AiCapabilityId("task-resolution-agent");

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

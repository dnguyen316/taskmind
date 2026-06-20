package com.taskmind.ai.contracts.activity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/** Nova-to-Core structured activity-search assist response. */
@JsonPropertyOrder({"query", "explanation", "suggestedFilters"})
public record ActivitySearchAssistResponse(
        String query, String explanation, List<String> suggestedFilters) {}

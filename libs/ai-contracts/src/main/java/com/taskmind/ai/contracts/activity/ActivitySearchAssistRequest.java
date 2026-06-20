package com.taskmind.ai.contracts.activity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.UUID;

/** Core-to-Nova request for turning natural-language activity search intent into a safe query. */
@JsonPropertyOrder({"userId", "prompt", "currentQuery", "supportedFilters"})
public record ActivitySearchAssistRequest(
        UUID userId, String prompt, String currentQuery, List<String> supportedFilters) {}

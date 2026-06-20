package com.taskmind.backend.activity.search;

import java.util.List;

public record ActivitySearchAssistResponse(
        String query, String explanation, List<String> suggestedFilters) {}

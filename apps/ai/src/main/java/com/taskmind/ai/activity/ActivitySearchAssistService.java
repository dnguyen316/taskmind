package com.taskmind.ai.activity;

import com.taskmind.ai.contracts.activity.ActivitySearchAssistRequest;
import com.taskmind.ai.contracts.activity.ActivitySearchAssistResponse;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ActivitySearchAssistService {
    private static final int MAX_QUERY_LENGTH = 300;
    private static final Pattern UNSAFE_QUERY_CHARS = Pattern.compile("[{}\\[\\]^~*?:\\\\]");
    private static final Set<String> SUPPORTED_FILTERS = Set.of();

    public ActivitySearchAssistResponse assist(ActivitySearchAssistRequest request) {
        String prompt = requireNonBlank(request.prompt(), "prompt");
        String currentQuery = request.currentQuery() == null ? "" : request.currentQuery().trim();
        List<String> requestedFilters = request.supportedFilters() == null ? List.of() : request.supportedFilters();
        List<String> unsupported = requestedFilters.stream().filter(filter -> !SUPPORTED_FILTERS.contains(filter)).toList();
        if (!unsupported.isEmpty()) {
            throw new IllegalArgumentException("Unsupported activity search filters: " + unsupported);
        }

        String query = sanitize(toQuery(prompt, currentQuery));
        if (query.isBlank()) {
            throw new IllegalArgumentException("Activity search assist produced an empty query");
        }
        return new ActivitySearchAssistResponse(
                query,
                "Nova converted the natural-language intent into a plain activity search query. Review before applying.",
                List.of());
    }

    private String toQuery(String prompt, String currentQuery) {
        String normalized = prompt.toLowerCase(Locale.ROOT);
        String query = prompt;
        query = query.replaceAll("(?i)\\b(show|find|search|look for|list|activity|events|where|that|are|were|was|the|my)\\b", " ");
        if (normalized.contains("done") || normalized.contains("completed")) {
            query = query + " DONE";
        }
        if (normalized.contains("todo") || normalized.contains("not started")) {
            query = query + " TODO";
        }
        if (normalized.contains("in progress") || normalized.contains("active")) {
            query = query + " IN_PROGRESS";
        }
        query = query.replaceAll("(?i)\\bfrom last week|last week|recent|recently|this week|today|yesterday\\b", " ");
        if (!currentQuery.isBlank() && !query.toLowerCase(Locale.ROOT).contains(currentQuery.toLowerCase(Locale.ROOT))) {
            query = currentQuery + " " + query;
        }
        return query;
    }

    private String sanitize(String value) {
        String sanitized = UNSAFE_QUERY_CHARS.matcher(value).replaceAll(" ");
        sanitized = sanitized.replaceAll("(?i)\\b(AND|OR|NOT)\\b", " ");
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        if (sanitized.length() > MAX_QUERY_LENGTH) {
            sanitized = sanitized.substring(0, MAX_QUERY_LENGTH).trim();
        }
        return sanitized;
    }

    private String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Activity search assist requires non-blank " + field);
        }
        return value.trim();
    }
}

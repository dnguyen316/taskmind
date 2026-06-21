package com.taskmind.backend.activity.search;

import com.taskmind.backend.ai.NovaClient;
import com.taskmind.backend.auth.AuthenticatedUser;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActivitySearchApplicationService {
    private final Optional<ActivitySearchRepository> repository;
    private final NovaClient novaClient;

    public ActivitySearchApplicationService(Optional<ActivitySearchRepository> repository, NovaClient novaClient) {
        this.repository = repository;
        this.novaClient = novaClient;
    }

    public List<ActivitySearchDocument> search(
            AuthenticatedUser requester,
            String query,
            int size,
            String entityType,
            String status,
            String projectId,
            String from,
            String to,
            String eventType) {
        return repository
                .orElseGet(DisabledActivitySearchRepository::new)
                .search(normalizeRequest(requester, query, size, entityType, status, projectId, from, to, eventType));
    }

    public List<String> suggest(
            AuthenticatedUser requester,
            String query,
            int size,
            String entityType,
            String status,
            String projectId,
            String from,
            String to,
            String eventType) {
        return repository
                .orElseGet(DisabledActivitySearchRepository::new)
                .suggest(normalizeRequest(requester, query, size, entityType, status, projectId, from, to, eventType));
    }

    public List<ActivitySearchSuggestion> recommend(
            AuthenticatedUser requester,
            String query,
            int size,
            String entityType,
            String status,
            String projectId,
            String from,
            String to,
            String eventType) {
        return repository
                .orElseGet(DisabledActivitySearchRepository::new)
                .recommend(normalizeRequest(requester, query, size, entityType, status, projectId, from, to, eventType));
    }

    public ActivitySearchAssistResponse assist(
            AuthenticatedUser requester, ActivitySearchAssistRequest request) {
        com.taskmind.ai.contracts.activity.ActivitySearchAssistResponse response =
                novaClient.assistActivitySearch(
                        new com.taskmind.ai.contracts.activity.ActivitySearchAssistRequest(
                                requester.userId(),
                                request.prompt(),
                                normalizeText(request.currentQuery()),
                                List.of("entityType", "status", "projectId", "from", "to", "eventType")));
        return new ActivitySearchAssistResponse(
                response.query(), response.explanation(), response.suggestedFilters());
    }

    private ActivitySearchRequest normalizeRequest(
            AuthenticatedUser requester,
            String query,
            int size,
            String entityType,
            String status,
            String projectId,
            String from,
            String to,
            String eventType) {
        Instant fromInstant = parseInstant("from", from);
        Instant toInstant = parseInstant("to", to);
        if (fromInstant != null && toInstant != null && fromInstant.isAfter(toInstant)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before or equal to to");
        }
        return new ActivitySearchRequest(
                requester.userId(),
                normalizeText(query),
                normalizeSize(size),
                normalizeText(entityType),
                normalizeText(status),
                parseUuid("projectId", projectId),
                fromInstant,
                toInstant,
                normalizeText(eventType));
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private UUID parseUuid(String field, String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be a valid UUID", e);
        }
    }

    private Instant parseInstant(String field, String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Instant.parse(normalized);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be an ISO-8601 instant", e);
        }
    }
}

package com.taskmind.backend.activity.search;

import com.taskmind.backend.ai.NovaClient;
import com.taskmind.backend.auth.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ActivitySearchApplicationService {
    private final Optional<ActivitySearchRepository> repository;
    private final NovaClient novaClient;

    public ActivitySearchApplicationService(Optional<ActivitySearchRepository> repository, NovaClient novaClient) {
        this.repository = repository;
        this.novaClient = novaClient;
    }

    public List<ActivitySearchDocument> search(AuthenticatedUser requester, String query, int size) {
        return repository
                .orElseGet(DisabledActivitySearchRepository::new)
                .search(requester.userId(), query, normalizeSize(size));
    }

    public List<String> suggest(AuthenticatedUser requester, String query, int size) {
        return repository
                .orElseGet(DisabledActivitySearchRepository::new)
                .suggest(requester.userId(), query, normalizeSize(size));
    }

    public ActivitySearchAssistResponse assist(
            AuthenticatedUser requester, ActivitySearchAssistRequest request) {
        com.taskmind.ai.contracts.activity.ActivitySearchAssistResponse response =
                novaClient.assistActivitySearch(
                        new com.taskmind.ai.contracts.activity.ActivitySearchAssistRequest(
                                requester.userId(),
                                request.prompt(),
                                request.currentQuery(),
                                List.of()));
        return new ActivitySearchAssistResponse(
                response.query(), response.explanation(), response.suggestedFilters());
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}

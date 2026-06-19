package com.taskmind.backend.activity.search;

import com.taskmind.backend.auth.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ActivitySearchApplicationService {
    private final Optional<ActivitySearchRepository> repository;

    public ActivitySearchApplicationService(Optional<ActivitySearchRepository> repository) {
        this.repository = repository;
    }

    public List<ActivitySearchDocument> search(
            AuthenticatedUser requester, String query, int size) {
        return repository
                .orElseGet(DisabledActivitySearchRepository::new)
                .search(requester.userId(), query, normalizeSize(size));
    }

    public List<String> suggest(AuthenticatedUser requester, String query, int size) {
        return repository
                .orElseGet(DisabledActivitySearchRepository::new)
                .suggest(requester.userId(), query, normalizeSize(size));
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}

package com.taskmind.backend.activity.search;

import java.util.List;
import java.util.UUID;

public interface ActivitySearchRepository {
    boolean enabled();

    List<ActivitySearchDocument> search(UUID userId, String query, int size);

    List<String> suggest(UUID userId, String query, int size);
}

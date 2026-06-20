package com.taskmind.backend.activity.search;

import java.util.List;
import java.util.UUID;

public interface ActivitySearchRepository {
    boolean enabled();

    List<ActivitySearchDocument> search(ActivitySearchRequest request);

    List<String> suggest(ActivitySearchRequest request);
}

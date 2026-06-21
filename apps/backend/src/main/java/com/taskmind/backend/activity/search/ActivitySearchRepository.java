package com.taskmind.backend.activity.search;

import java.util.List;

public interface ActivitySearchRepository {
    boolean enabled();

    List<ActivitySearchDocument> search(ActivitySearchRequest request);

    List<String> suggest(ActivitySearchRequest request);

    List<ActivitySearchSuggestion> recommend(ActivitySearchRequest request);
}

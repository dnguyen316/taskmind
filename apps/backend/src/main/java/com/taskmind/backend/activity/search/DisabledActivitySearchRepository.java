package com.taskmind.backend.activity.search;

import java.util.List;
import java.util.UUID;

public class DisabledActivitySearchRepository implements ActivitySearchRepository {
    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public List<ActivitySearchDocument> search(UUID userId, String query, int size) {
        throw new ActivitySearchDisabledException();
    }
}

package com.taskmind.backend.activity.domain.repository;

import com.taskmind.backend.activity.domain.model.ActivityEvent;

public interface ActivityEventRepository {
    ActivityEvent save(ActivityEvent event);
}

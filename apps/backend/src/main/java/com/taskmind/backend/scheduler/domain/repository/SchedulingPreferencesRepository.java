package com.taskmind.backend.scheduler.domain.repository;

import com.taskmind.backend.scheduler.domain.model.SchedulingPreferences;
import java.util.Optional;
import java.util.UUID;

public interface SchedulingPreferencesRepository {
    SchedulingPreferences save(SchedulingPreferences preferences);

    Optional<SchedulingPreferences> findByUserId(UUID userId);

    Optional<SchedulingPreferences> findByUserIdForUpdate(UUID userId);
}

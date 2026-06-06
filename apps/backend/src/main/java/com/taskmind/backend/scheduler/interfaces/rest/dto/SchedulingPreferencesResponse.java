package com.taskmind.backend.scheduler.interfaces.rest.dto;

import com.taskmind.backend.scheduler.domain.model.SchedulingPreferences;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record SchedulingPreferencesResponse(
        UUID id,
        Long version,
        UUID userId,
        LocalTime workdayStart,
        LocalTime workdayEnd,
        int blockGranularityMinutes,
        int maxDailyFocusMinutes,
        Instant createdAt,
        Instant updatedAt) {
    public static SchedulingPreferencesResponse fromDomain(SchedulingPreferences preferences) {
        return new SchedulingPreferencesResponse(
                preferences.id(),
                preferences.version(),
                preferences.userId(),
                preferences.workdayStart(),
                preferences.workdayEnd(),
                preferences.blockGranularityMinutes(),
                preferences.maxDailyFocusMinutes(),
                preferences.createdAt(),
                preferences.updatedAt());
    }
}

package com.taskmind.backend.scheduler.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record SchedulingPreferences(
        UUID id,
        @JsonIgnore Long version,
        UUID userId,
        LocalTime workdayStart,
        LocalTime workdayEnd,
        int blockGranularityMinutes,
        int maxDailyFocusMinutes,
        Instant createdAt,
        Instant updatedAt) {
    public static final LocalTime DEFAULT_WORKDAY_START = LocalTime.of(9, 0);
    public static final LocalTime DEFAULT_WORKDAY_END = LocalTime.of(17, 0);
    public static final int DEFAULT_BLOCK_GRANULARITY_MINUTES = 30;
    public static final int DEFAULT_MAX_DAILY_FOCUS_MINUTES = 360;

    public SchedulingPreferences {
        if (userId == null)
            throw new IllegalArgumentException("Scheduling preference user is required");
        if (workdayStart == null || workdayEnd == null || !workdayStart.isBefore(workdayEnd)) {
            throw new IllegalArgumentException("Scheduling preference workday window is invalid");
        }
        if (blockGranularityMinutes < 15 || blockGranularityMinutes > 240) {
            throw new IllegalArgumentException(
                    "Block granularity must be between 15 and 240 minutes");
        }
        if (maxDailyFocusMinutes < 15 || maxDailyFocusMinutes > 1440) {
            throw new IllegalArgumentException("Daily focus minutes must be between 15 and 1440");
        }
    }

    public static SchedulingPreferences defaultsFor(UUID userId, Instant now) {
        return new SchedulingPreferences(
                UUID.randomUUID(),
                null,
                userId,
                DEFAULT_WORKDAY_START,
                DEFAULT_WORKDAY_END,
                DEFAULT_BLOCK_GRANULARITY_MINUTES,
                DEFAULT_MAX_DAILY_FOCUS_MINUTES,
                now,
                now);
    }

    public SchedulingPreferences updated(
            LocalTime start, LocalTime end, int granularity, int maxFocus, Instant now) {
        return new SchedulingPreferences(
                id, version, userId, start, end, granularity, maxFocus, createdAt, now);
    }
}

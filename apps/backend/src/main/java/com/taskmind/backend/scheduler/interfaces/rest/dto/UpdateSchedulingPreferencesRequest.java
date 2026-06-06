package com.taskmind.backend.scheduler.interfaces.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record UpdateSchedulingPreferencesRequest(
        Long version,
        @NotNull LocalTime workdayStart,
        @NotNull LocalTime workdayEnd,
        @Min(15) @Max(240) int blockGranularityMinutes,
        @Min(15) @Max(1440) int maxDailyFocusMinutes) {}

package com.taskmind.backend.analytics.application;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public enum ReportsRange {
    WEEK(7),
    MONTH(30),
    QUARTER(90);
    private final int days;

    ReportsRange(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }

    public LocalDate start(LocalDate today) {
        return today.minusDays(days - 1L);
    }

    public static ReportsRange from(String value) {
        if (value == null || value.isBlank()) {
            return WEEK;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(range -> range.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new InvalidReportsRangeException(normalized, allowedValues()));
    }

    public static List<String> allowedValues() {
        return Arrays.stream(values()).map(range -> range.name().toLowerCase()).toList();
    }
}

package com.taskmind.backend.analytics.application;

import java.time.LocalDate;

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
        return value == null ? WEEK : ReportsRange.valueOf(value.trim().toUpperCase());
    }
}

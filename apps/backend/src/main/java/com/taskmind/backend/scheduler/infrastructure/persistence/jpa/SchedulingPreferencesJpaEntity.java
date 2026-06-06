package com.taskmind.backend.scheduler.infrastructure.persistence.jpa;

import com.taskmind.backend.scheduler.domain.model.SchedulingPreferences;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "scheduling_preferences")
public class SchedulingPreferencesJpaEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "workday_start", nullable = false)
    private LocalTime workdayStart;

    @Column(name = "workday_end", nullable = false)
    private LocalTime workdayEnd;

    @Column(name = "block_granularity_minutes", nullable = false)
    private int blockGranularityMinutes;

    @Column(name = "max_daily_focus_minutes", nullable = false)
    private int maxDailyFocusMinutes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SchedulingPreferencesJpaEntity() {}

    private SchedulingPreferencesJpaEntity(SchedulingPreferences preferences) {
        id = preferences.id();
        version = preferences.version();
        userId = preferences.userId();
        workdayStart = preferences.workdayStart();
        workdayEnd = preferences.workdayEnd();
        blockGranularityMinutes = preferences.blockGranularityMinutes();
        maxDailyFocusMinutes = preferences.maxDailyFocusMinutes();
        createdAt = preferences.createdAt();
        updatedAt = preferences.updatedAt();
    }

    public static SchedulingPreferencesJpaEntity fromDomain(SchedulingPreferences preferences) {
        return new SchedulingPreferencesJpaEntity(preferences);
    }

    public SchedulingPreferences toDomain() {
        return new SchedulingPreferences(
                id,
                version,
                userId,
                workdayStart,
                workdayEnd,
                blockGranularityMinutes,
                maxDailyFocusMinutes,
                createdAt,
                updatedAt);
    }
}

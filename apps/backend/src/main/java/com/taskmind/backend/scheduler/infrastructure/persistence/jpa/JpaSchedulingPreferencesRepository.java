package com.taskmind.backend.scheduler.infrastructure.persistence.jpa;

import com.taskmind.backend.scheduler.domain.model.SchedulingPreferences;
import com.taskmind.backend.scheduler.domain.repository.SchedulingPreferencesRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSchedulingPreferencesRepository implements SchedulingPreferencesRepository {
    private final SpringDataSchedulingPreferencesJpaRepository repo;

    public JpaSchedulingPreferencesRepository(SpringDataSchedulingPreferencesJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public SchedulingPreferences save(SchedulingPreferences preferences) {
        return repo.saveAndFlush(SchedulingPreferencesJpaEntity.fromDomain(preferences)).toDomain();
    }

    @Override
    public Optional<SchedulingPreferences> findByUserId(UUID userId) {
        return repo.findByUserId(userId).map(SchedulingPreferencesJpaEntity::toDomain);
    }

    @Override
    public Optional<SchedulingPreferences> findByUserIdForUpdate(UUID userId) {
        return repo.findByUserIdForUpdate(userId).map(SchedulingPreferencesJpaEntity::toDomain);
    }
}

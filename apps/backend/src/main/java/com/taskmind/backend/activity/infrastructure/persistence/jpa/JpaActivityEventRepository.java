package com.taskmind.backend.activity.infrastructure.persistence.jpa;

import com.taskmind.backend.activity.domain.model.ActivityEvent;
import com.taskmind.backend.activity.domain.repository.ActivityEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaActivityEventRepository implements ActivityEventRepository {
    private final SpringDataActivityEventJpaRepository repository;

    public JpaActivityEventRepository(SpringDataActivityEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ActivityEvent save(ActivityEvent event) {
        return repository.save(ActivityEventJpaEntity.fromDomain(event)).toDomain();
    }
}

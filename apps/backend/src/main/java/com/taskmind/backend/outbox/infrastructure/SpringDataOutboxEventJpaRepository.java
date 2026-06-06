package com.taskmind.backend.outbox.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOutboxEventJpaRepository
        extends JpaRepository<OutboxEventJpaEntity, UUID> {
    List<OutboxEventJpaEntity> findByPublishedAtIsNullOrderByOccurredAtAsc(Pageable pageable);

    long countByPublishedAtIsNull();
}

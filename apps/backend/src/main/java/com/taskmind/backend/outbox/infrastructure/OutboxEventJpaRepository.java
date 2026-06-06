package com.taskmind.backend.outbox.infrastructure;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxEventJpaRepository {
    private final SpringDataOutboxEventJpaRepository repository;

    public OutboxEventJpaRepository(SpringDataOutboxEventJpaRepository repository) {
        this.repository = repository;
    }

    public OutboxEventJpaEntity save(OutboxEventJpaEntity event) {
        return repository.save(event);
    }

    public List<OutboxEventJpaEntity> findUnpublished(int limit) {
        return repository.findByPublishedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, limit));
    }

    public long unpublishedCount() {
        return repository.countByPublishedAtIsNull();
    }
}

package com.taskmind.backend.outbox.infrastructure;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OutboxEventJpaRepository {
    private final SpringDataOutboxEventJpaRepository repository;

    public OutboxEventJpaRepository(SpringDataOutboxEventJpaRepository repository) {
        this.repository = repository;
    }

    public OutboxEventJpaEntity save(OutboxEventJpaEntity event) {
        return repository.save(event);
    }

    public List<OutboxEventJpaEntity> findClaimedBy(String pollerId) {
        return repository.findByClaimedByAndPublishedAtIsNullOrderByOccurredAtAsc(pollerId);
    }

    @Transactional
    public List<OutboxEventJpaEntity> claimPending(String pollerId, Instant claimedAt, int limit) {
        repository.claimPending(pollerId, claimedAt, limit);
        return findClaimedBy(pollerId);
    }

    public long unpublishedCount() {
        return repository.countByPublishedAtIsNull();
    }

    public long pendingCount() {
        return repository.countByPublishedAtIsNullAndClaimedAtIsNull();
    }
}

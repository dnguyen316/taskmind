package com.taskmind.backend.outbox.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataOutboxEventJpaRepository
        extends JpaRepository<OutboxEventJpaEntity, UUID> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value =
                    """
                    update outbox_events
                    set claimed_at = :claimedAt,
                        claimed_by = :claimedBy,
                        updated_at = :claimedAt
                    where published_at is null
                      and claimed_at is null
                      and id in (
                        select id
                        from outbox_events
                        where published_at is null
                          and claimed_at is null
                        order by occurred_at asc
                        limit :limit
                    )
                    """,
            nativeQuery = true)
    int claimPending(
            @Param("claimedBy") String claimedBy,
            @Param("claimedAt") Instant claimedAt,
            @Param("limit") int limit);

    List<OutboxEventJpaEntity> findByClaimedByAndPublishedAtIsNullOrderByOccurredAtAsc(
            String claimedBy);

    long countByPublishedAtIsNull();

    long countByPublishedAtIsNullAndClaimedAtIsNull();
}

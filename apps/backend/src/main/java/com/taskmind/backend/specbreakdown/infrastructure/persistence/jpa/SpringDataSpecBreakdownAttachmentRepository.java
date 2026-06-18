package com.taskmind.backend.specbreakdown.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataSpecBreakdownAttachmentRepository extends JpaRepository<SpecBreakdownAttachmentJpaEntity, UUID> {
    @Query("select a from SpecBreakdownAttachmentJpaEntity a where a.id=:id and a.deletedAt is null")
    Optional<SpecBreakdownAttachmentJpaEntity> findActiveById(@Param("id") UUID id);

    @Query(
            "select a from SpecBreakdownAttachmentJpaEntity a where a.draftId=:draftId and a.deletedAt is null order by a.createdAt desc")
    List<SpecBreakdownAttachmentJpaEntity> findActiveByDraftId(@Param("draftId") UUID draftId);
}

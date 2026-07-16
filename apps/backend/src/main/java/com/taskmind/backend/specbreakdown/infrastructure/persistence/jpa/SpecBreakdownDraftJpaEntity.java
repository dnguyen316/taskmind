package com.taskmind.backend.specbreakdown.infrastructure.persistence.jpa;

import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownDraft;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spec_breakdown_drafts")
public class SpecBreakdownDraftJpaEntity {
    @Id
    UUID id;

    @Version
    Long version;

    UUID projectId;
    UUID ownerUserId;
    UUID templateId;
    String title;

    @Column(columnDefinition = "TEXT")
    String rawSpec;

    @Column(columnDefinition = "TEXT")
    String richContent;

    @Column(columnDefinition = "TEXT")
    String candidateTree;

    @Enumerated(EnumType.STRING)
    SpecBreakdownStatus status;

    String fixVersion;
    String affectedVersion;
    String sprint;
    String issueType;
    String publishKey;
    Instant materializedAt;
    Instant createdAt;
    Instant updatedAt;

    protected SpecBreakdownDraftJpaEntity() {}

    static SpecBreakdownDraftJpaEntity from(SpecBreakdownDraft draft) {
        SpecBreakdownDraftJpaEntity entity = new SpecBreakdownDraftJpaEntity();
        entity.id = draft.id();
        entity.version = draft.version();
        entity.projectId = draft.projectId();
        entity.ownerUserId = draft.ownerUserId();
        entity.templateId = draft.templateId();
        entity.title = draft.title();
        entity.rawSpec = draft.rawSpec();
        entity.richContent = draft.richContent();
        entity.candidateTree = draft.candidateTree();
        entity.status = draft.status();
        entity.fixVersion = draft.fixVersion();
        entity.affectedVersion = draft.affectedVersion();
        entity.sprint = draft.sprint();
        entity.issueType = draft.issueType();
        entity.publishKey = draft.publishKey();
        entity.materializedAt = draft.materializedAt();
        entity.createdAt = draft.createdAt();
        entity.updatedAt = draft.updatedAt();
        return entity;
    }

    SpecBreakdownDraft toDomain() {
        return new SpecBreakdownDraft(
                id,
                version,
                projectId,
                ownerUserId,
                templateId,
                title,
                rawSpec,
                richContent,
                candidateTree,
                status,
                fixVersion,
                affectedVersion,
                sprint,
                issueType,
                publishKey,
                materializedAt,
                createdAt,
                updatedAt);
    }
}

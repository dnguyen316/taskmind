package com.taskmind.backend.specbreakdown.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SpecBreakdownDraft(
        UUID id,
        Long version,
        UUID projectId,
        UUID ownerUserId,
        UUID templateId,
        String title,
        String rawSpec,
        String richContent,
        String candidateTree,
        SpecBreakdownStatus status,
        String fixVersion,
        String affectedVersion,
        String sprint,
        String issueType,
        String publishKey,
        Instant materializedAt,
        Instant createdAt,
        Instant updatedAt) {}

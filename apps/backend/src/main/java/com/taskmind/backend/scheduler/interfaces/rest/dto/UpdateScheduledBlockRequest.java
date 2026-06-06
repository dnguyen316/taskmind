package com.taskmind.backend.scheduler.interfaces.rest.dto;

import java.time.OffsetDateTime;

public record UpdateScheduledBlockRequest(
        Long version, OffsetDateTime startsAt, OffsetDateTime endsAt, String rationale) {}

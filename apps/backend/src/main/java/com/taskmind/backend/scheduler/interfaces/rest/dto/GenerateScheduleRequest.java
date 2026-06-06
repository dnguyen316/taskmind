package com.taskmind.backend.scheduler.interfaces.rest.dto;

import java.time.OffsetDateTime;

public record GenerateScheduleRequest(OffsetDateTime from, OffsetDateTime to) {}

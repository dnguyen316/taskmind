package com.taskmind.backend.scheduler.application;

import java.time.OffsetDateTime;

public record GenerateScheduleCommand(OffsetDateTime from, OffsetDateTime to) {}

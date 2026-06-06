package com.taskmind.backend.scheduler.interfaces.rest.dto;

import com.taskmind.backend.scheduler.domain.RescheduleProposalEngine.RescheduleProposal;
import java.util.List;

public record GenerateScheduleResponse(
        List<ScheduledBlockResponse> blocks, List<RescheduleProposal> proposals) {}

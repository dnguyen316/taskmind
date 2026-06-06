package com.taskmind.backend.scheduler.domain;

import com.taskmind.backend.scheduler.domain.model.ScheduledBlock;
import com.taskmind.backend.scheduler.domain.model.ScheduledBlockStatus;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RescheduleProposalEngine {
    public List<RescheduleProposal> propose(List<ScheduledBlock> blocks, OffsetDateTime now) {
        return blocks.stream()
                .filter(
                        block ->
                                block.status() == ScheduledBlockStatus.MISSED
                                        || (block.status() == ScheduledBlockStatus.SCHEDULED
                                                && block.endsAt().isBefore(now)))
                .sorted(Comparator.comparing(ScheduledBlock::startsAt))
                .map(
                        block ->
                                new RescheduleProposal(
                                        block.id(),
                                        block.taskId(),
                                        "Block is overdue and should be moved"))
                .toList();
    }

    public record RescheduleProposal(
            java.util.UUID blockId, java.util.UUID taskId, String reason) {}
}

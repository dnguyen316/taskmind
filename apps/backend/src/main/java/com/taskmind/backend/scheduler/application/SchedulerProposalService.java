package com.taskmind.backend.scheduler.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.scheduler.domain.RescheduleProposalEngine;
import com.taskmind.backend.scheduler.domain.RescheduleProposalEngine.RescheduleProposal;
import com.taskmind.backend.scheduler.domain.repository.ScheduledBlockRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SchedulerProposalService {
    private final ScheduledBlockRepository blocks;
    private final RescheduleProposalEngine engine;
    private final SchedulerCommandService commands;

    public SchedulerProposalService(
            ScheduledBlockRepository blocks,
            RescheduleProposalEngine engine,
            SchedulerCommandService commands) {
        this.blocks = blocks;
        this.engine = engine;
        this.commands = commands;
    }

    @Transactional
    public List<RescheduleProposal> overdueProposals(AuthenticatedUser requester) {
        var now = OffsetDateTime.now();
        commands.markMissedBlocks(requester, now);
        return engine.propose(
                blocks.findByUserIdBetween(requester.userId(), now.minusDays(30), now), now);
    }
}

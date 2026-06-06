package com.taskmind.backend.scheduler.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.scheduler.application.GenerateScheduleCommand;
import com.taskmind.backend.scheduler.application.SchedulerCommandService;
import com.taskmind.backend.scheduler.application.SchedulerProposalService;
import com.taskmind.backend.scheduler.interfaces.rest.dto.GenerateScheduleRequest;
import com.taskmind.backend.scheduler.interfaces.rest.dto.GenerateScheduleResponse;
import com.taskmind.backend.scheduler.interfaces.rest.dto.ScheduledBlockResponse;
import com.taskmind.backend.scheduler.interfaces.rest.dto.SchedulingPreferencesResponse;
import com.taskmind.backend.scheduler.interfaces.rest.dto.UpdateScheduledBlockRequest;
import com.taskmind.backend.scheduler.interfaces.rest.dto.UpdateSchedulingPreferencesRequest;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/scheduler")
public class SchedulerController {
    private final SchedulerCommandService commands;
    private final SchedulerProposalService proposals;

    public SchedulerController(
            SchedulerCommandService commands, SchedulerProposalService proposals) {
        this.commands = commands;
        this.proposals = proposals;
    }

    @GetMapping("/preferences")
    public SchedulingPreferencesResponse getPreferences(AuthenticatedUser requester) {
        return SchedulingPreferencesResponse.fromDomain(commands.preferencesFor(requester));
    }

    @PutMapping("/preferences")
    public SchedulingPreferencesResponse updatePreferences(
            AuthenticatedUser requester,
            @Valid @RequestBody UpdateSchedulingPreferencesRequest request) {
        try {
            return SchedulingPreferencesResponse.fromDomain(
                    commands.updatePreferences(
                            requester,
                            request.version(),
                            request.workdayStart(),
                            request.workdayEnd(),
                            request.blockGranularityMinutes(),
                            request.maxDailyFocusMinutes()));
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Scheduling preferences were updated by another request",
                    e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/generate")
    public GenerateScheduleResponse generate(
            AuthenticatedUser requester,
            @RequestBody(required = false) GenerateScheduleRequest request) {
        GenerateScheduleRequest body =
                request == null ? new GenerateScheduleRequest(null, null) : request;
        try {
            List<ScheduledBlockResponse> generated =
                    commands
                            .generate(
                                    requester, new GenerateScheduleCommand(body.from(), body.to()))
                            .stream()
                            .map(ScheduledBlockResponse::fromDomain)
                            .toList();
            return new GenerateScheduleResponse(generated, proposals.overdueProposals(requester));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/blocks")
    public List<ScheduledBlockResponse> blocks(
            AuthenticatedUser requester,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {
        return commands.listBlocks(requester, from, to).stream()
                .map(ScheduledBlockResponse::fromDomain)
                .toList();
    }

    @PatchMapping("/blocks/{id}")
    public ResponseEntity<ScheduledBlockResponse> updateBlock(
            AuthenticatedUser requester,
            @PathVariable UUID id,
            @RequestBody UpdateScheduledBlockRequest request) {
        try {
            return commands.updateBlock(
                            requester,
                            id,
                            request.version(),
                            request.startsAt(),
                            request.endsAt(),
                            request.rationale())
                    .map(ScheduledBlockResponse::fromDomain)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Scheduled block was updated by another request", e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }

    @PostMapping("/blocks/{id}/complete")
    public ResponseEntity<ScheduledBlockResponse> completeBlock(
            AuthenticatedUser requester, @PathVariable UUID id) {
        try {
            return commands.completeBlock(requester, id)
                    .map(ScheduledBlockResponse::fromDomain)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }
}

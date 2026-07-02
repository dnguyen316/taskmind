package com.taskmind.backend.aiproposal.interfaces.rest;

import com.taskmind.ai.contracts.proposal.AiActionProposalContract;
import com.taskmind.backend.aiproposal.application.AiActionProposalApplicationService;
import com.taskmind.backend.aiproposal.application.AiProposalImpactPreview;
import com.taskmind.backend.aiproposal.interfaces.rest.dto.AiActionProposalDecisionRequest;
import com.taskmind.backend.auth.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/ai/proposals")
public class AiActionProposalController {
    private final AiActionProposalApplicationService service;

    public AiActionProposalController(AiActionProposalApplicationService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    public List<AiActionProposalContract> pending(AuthenticatedUser user) {
        return service.listPending(user);
    }

    @GetMapping("/{proposalId}/preview")
    public ResponseEntity<AiProposalImpactPreview> preview(
            AuthenticatedUser user, @PathVariable UUID proposalId) {
        return ResponseEntity.of(service.preview(user, proposalId));
    }

    @PostMapping("/{proposalId}/accept")
    public ResponseEntity<AiActionProposalContract> accept(
            AuthenticatedUser user, @PathVariable UUID proposalId) {
        return ResponseEntity.of(service.accept(user, proposalId));
    }

    @PostMapping("/{proposalId}/reject")
    public ResponseEntity<AiActionProposalContract> reject(
            AuthenticatedUser user,
            @PathVariable UUID proposalId,
            @RequestBody(required = false) AiActionProposalDecisionRequest request) {
        return ResponseEntity.of(
                service.reject(user, proposalId, request == null ? null : request.reason()));
    }

    @PostMapping("/{proposalId}/accept-with-edits")
    public ResponseEntity<AiActionProposalContract> acceptWithEdits(
            AuthenticatedUser user,
            @PathVariable UUID proposalId,
            @RequestBody AiActionProposalDecisionRequest request) {
        return ResponseEntity.of(
                service.acceptWithEdits(user, proposalId, request.editedPayload()));
    }
}

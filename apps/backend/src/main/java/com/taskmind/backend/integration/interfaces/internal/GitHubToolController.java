package com.taskmind.backend.integration.interfaces.internal;

import com.taskmind.backend.integration.application.GitHubToolApplicationService;
import com.taskmind.backend.integration.application.GitHubToolApplicationService.GitHubToolException;
import com.taskmind.backend.integration.infrastructure.ProviderClientException;
import com.taskmind.backend.integration.infrastructure.github.GitHubClient;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/integrations/github/repos/{linkId}")
public class GitHubToolController {
    private static final String USER_HEADER = "X-TaskMind-User-Id";
    private static final String PROJECT_HEADER = "X-TaskMind-Project-Id";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private final GitHubToolApplicationService service;

    public GitHubToolController(GitHubToolApplicationService service) { this.service = service; }

    @GetMapping("/issues/{issueNumber}")
    public GitHubClient.Issue issue(@PathVariable UUID linkId, @PathVariable int issueNumber, @RequestHeader(USER_HEADER) UUID userId, @RequestHeader(PROJECT_HEADER) UUID projectId) {
        return service.getIssue(linkId, userId, projectId, issueNumber);
    }

    @GetMapping("/pulls/{pullNumber}")
    public GitHubClient.PullRequestStatus pull(@PathVariable UUID linkId, @PathVariable int pullNumber, @RequestHeader(USER_HEADER) UUID userId, @RequestHeader(PROJECT_HEADER) UUID projectId) {
        return service.getPullRequest(linkId, userId, projectId, pullNumber);
    }

    @PostMapping("/comments")
    public GitHubClient.Comment comment(@PathVariable UUID linkId, @RequestHeader(USER_HEADER) UUID userId, @RequestHeader(PROJECT_HEADER) UUID projectId, @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey, @RequestBody CommentRequest request) {
        return service.createComment(linkId, userId, projectId, idempotencyKey, request.issueNumber(), request.body());
    }

    @PostMapping("/branches")
    public GitHubClient.Ref branch(@PathVariable UUID linkId, @RequestHeader(USER_HEADER) UUID userId, @RequestHeader(PROJECT_HEADER) UUID projectId, @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey, @RequestBody BranchRequest request) {
        return service.createBranch(linkId, userId, projectId, idempotencyKey, request.branchName(), request.fromSha());
    }

    @PostMapping("/pull-requests")
    public GitHubClient.PullRequest pullRequest(@PathVariable UUID linkId, @RequestHeader(USER_HEADER) UUID userId, @RequestHeader(PROJECT_HEADER) UUID projectId, @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey, @RequestBody PullRequestRequest request) {
        return service.createPullRequest(linkId, userId, projectId, idempotencyKey, request.title(), request.head(), request.base(), request.body());
    }

    @ExceptionHandler(GitHubToolException.Forbidden.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ProblemDetail forbidden(RuntimeException ex) { return problem(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage()); }

    @ExceptionHandler(GitHubToolException.NotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail notFound(RuntimeException ex) { return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage()); }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail badRequest(RuntimeException ex) { return problem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage()); }

    @ExceptionHandler(ProviderClientException.class)
    ResponseEntity<ProblemDetail> provider(ProviderClientException ex) {
        HttpStatus status = "PROVIDER_RATE_LIMITED".equals(ex.errorCode()) ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_GATEWAY;
        ProblemDetail problem = problem(status, "Integration provider request failed", ex.getMessage());
        problem.setProperty("code", ex.errorCode());
        problem.setProperty("providerStatus", ex.statusCode().value());
        problem.setProperty("retrySafe", ex.retrySafe());
        return ResponseEntity.status(status).body(problem);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

    public record CommentRequest(int issueNumber, String body) {}
    public record BranchRequest(String branchName, String fromSha) {}
    public record PullRequestRequest(String title, String head, String base, String body) {}
}

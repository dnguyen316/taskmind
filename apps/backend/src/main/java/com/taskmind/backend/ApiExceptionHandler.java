package com.taskmind.backend;

import com.taskmind.backend.config.logging.ProblemDetailLogging;
import com.taskmind.backend.integration.infrastructure.ProviderClientException;
import com.taskmind.backend.task.application.TaskAccessDeniedException;
import com.taskmind.backend.task.application.TaskErrorCode;
import com.taskmind.backend.task.application.TaskErrorMetadata;
import com.taskmind.backend.task.application.TaskNotFoundException;
import com.taskmind.backend.task.application.TaskValidationException;
import com.taskmind.backend.tasktype.application.TaskTypeForbiddenException;
import com.taskmind.backend.tasktype.application.TaskTypeValidationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final ProblemDetailLogging problemDetailLogging;

    public ApiExceptionHandler(ObjectProvider<ProblemDetailLogging> problemDetailLogging) {
        this.problemDetailLogging = problemDetailLogging.getIfAvailable(ProblemDetailLogging::new);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Concurrent update conflict");
        problemDetail.setDetail("The resource was updated by another request. Refresh and retry.");
        return problemDetailLogging.enrich(problemDetail);
    }

    @ExceptionHandler(TaskValidationException.class)
    public ProblemDetail handleTaskValidation(TaskValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid task request", ex.getMessage(), ex.metadata());
    }

    @ExceptionHandler(TaskAccessDeniedException.class)
    public ProblemDetail handleTaskAccessDenied(TaskAccessDeniedException ex) {
        return problem(
                HttpStatus.FORBIDDEN,
                "Task access denied",
                "You are not allowed to perform this task operation.",
                ex.metadata().sanitizedForAccessDenied());
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ProblemDetail handleTaskNotFound(TaskNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Task not found", ex.getMessage(), ex.metadata());
    }

    @ExceptionHandler(TaskTypeForbiddenException.class)
    public ProblemDetail handleTaskTypeForbidden(TaskTypeForbiddenException ex) {
        return problem(HttpStatus.FORBIDDEN, "Task type access denied", ex.getMessage(), "TASK_TYPE_FORBIDDEN");
    }

    @ExceptionHandler(TaskTypeValidationException.class)
    public ProblemDetail handleTaskTypeValidation(TaskTypeValidationException ex) {
        ProblemDetail problemDetail = problem(HttpStatus.BAD_REQUEST, "Invalid task type request", ex.getMessage(), ex.metadata());
        setIfPresent(problemDetail, "field", ex.field());
        setIfPresent(problemDetail, "reason", ex.reason());
        if (ex.projectId() != null) {
            problemDetail.setProperty("projectId", ex.projectId().toString());
        }
        return problemDetail;
    }

    @ExceptionHandler(ProviderClientException.class)
    public ProblemDetail handleProviderClientException(ProviderClientException ex) {
        log.warn(
                "provider_client_exception code={} providerStatus={} retrySafe={} correlationId={}",
                ex.errorCode(),
                ex.statusCode().value(),
                ex.retrySafe(),
                com.taskmind.backend.config.logging.RequestCorrelation.currentId(),
                ex);
        HttpStatus status = "PROVIDER_RATE_LIMITED".equals(ex.errorCode()) ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_GATEWAY;
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setTitle("Integration provider request failed");
        problemDetail.setDetail("Integration provider request failed.");
        problemDetail.setProperty("code", ex.errorCode());
        problemDetail.setProperty("providerStatus", ex.statusCode().value());
        problemDetail.setProperty("retrySafe", ex.retrySafe());
        return problemDetailLogging.enrich(problemDetail);
    }


    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex) {
        if (ex.getCause() != null) {
            log.warn(
                    "public_api_exception status={} code={} correlationId={}",
                    ex.getStatusCode().value(),
                    errorCodeFor(ex),
                    com.taskmind.backend.config.logging.RequestCorrelation.currentId(),
                    ex);
        }
        ProblemDetail problemDetail = ProblemDetail.forStatus(ex.getStatusCode());
        problemDetail.setTitle(titleFor(ex));
        problemDetail.setDetail(detailFor(ex));
        problemDetail.setProperty("code", errorCodeFor(ex));
        return problemDetailLogging.enrich(problemDetail);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        return problem(status, title, detail, TaskErrorMetadata.withCode(TaskErrorCode.valueOf(code)));
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, TaskErrorMetadata metadata) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        setIfPresent(problemDetail, "code", metadata.code().name());
        setIfPresent(problemDetail, "resource", metadata.resource());
        setIfPresent(problemDetail, "resourceId", metadata.resourceId());
        setIfPresent(problemDetail, "operation", metadata.operation());
        setIfPresent(problemDetail, "field", metadata.field());
        setIfPresent(problemDetail, "reason", metadata.reason());
        return problemDetailLogging.enrich(problemDetail);
    }

    private String errorCodeFor(ResponseStatusException ex) {
        return switch (ex.getStatusCode().value()) {
            case 400 -> "PUBLIC_REQUEST_INVALID";
            case 403 -> "PUBLIC_ACCESS_DENIED";
            case 404 -> "PUBLIC_RESOURCE_NOT_FOUND";
            case 409 -> "PUBLIC_REQUEST_CONFLICT";
            case 413 -> "PUBLIC_UPLOAD_TOO_LARGE";
            case 503 -> "PUBLIC_DEPENDENCY_UNAVAILABLE";
            default -> "PUBLIC_REQUEST_FAILED";
        };
    }

    private String titleFor(ResponseStatusException ex) {
        return switch (ex.getStatusCode().value()) {
            case 400 -> "Invalid request";
            case 403 -> "Access denied";
            case 404 -> "Resource not found";
            case 409 -> "Request conflict";
            case 413 -> "Upload too large";
            case 503 -> "Service unavailable";
            default -> "Request failed";
        };
    }

    private String detailFor(ResponseStatusException ex) {
        return switch (ex.getStatusCode().value()) {
            case 400 -> "The request could not be processed.";
            case 403 -> "You are not allowed to perform this operation.";
            case 404 -> "The requested resource was not found.";
            case 409 -> "The request conflicts with the current resource state.";
            case 413 -> "The uploaded file is too large.";
            case 503 -> "A dependent service is temporarily unavailable.";
            default -> "The request failed.";
        };
    }

    private void setIfPresent(ProblemDetail problemDetail, String name, String value) {
        if (value != null) {
            problemDetail.setProperty(name, value);
        }
    }
}

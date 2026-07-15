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

@RestControllerAdvice
public class ApiExceptionHandler {
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
        HttpStatus status = "PROVIDER_RATE_LIMITED".equals(ex.errorCode()) ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_GATEWAY;
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setTitle("Integration provider request failed");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setProperty("code", ex.errorCode());
        problemDetail.setProperty("providerStatus", ex.statusCode().value());
        problemDetail.setProperty("retrySafe", ex.retrySafe());
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

    private void setIfPresent(ProblemDetail problemDetail, String name, String value) {
        if (value != null) {
            problemDetail.setProperty(name, value);
        }
    }
}

package com.taskmind.backend;

import com.taskmind.backend.integration.infrastructure.ProviderClientException;
import com.taskmind.backend.task.application.TaskAccessDeniedException;
import com.taskmind.backend.task.application.TaskNotFoundException;
import com.taskmind.backend.task.application.TaskValidationException;
import com.taskmind.backend.tasktype.application.TaskTypeForbiddenException;
import com.taskmind.backend.tasktype.application.TaskTypeValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Concurrent update conflict");
        problemDetail.setDetail("The resource was updated by another request. Refresh and retry.");
        return problemDetail;
    }

    @ExceptionHandler(TaskValidationException.class)
    public ProblemDetail handleTaskValidation(TaskValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid task request", ex.getMessage(), "TASK_VALIDATION_FAILED");
    }

    @ExceptionHandler(TaskAccessDeniedException.class)
    public ProblemDetail handleTaskAccessDenied(TaskAccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Task access denied", ex.getMessage(), "TASK_ACCESS_DENIED");
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ProblemDetail handleTaskNotFound(TaskNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Task not found", ex.getMessage(), "TASK_NOT_FOUND");
    }

    @ExceptionHandler(TaskTypeForbiddenException.class)
    public ProblemDetail handleTaskTypeForbidden(TaskTypeForbiddenException ex) {
        return problem(HttpStatus.FORBIDDEN, "Task type access denied", ex.getMessage(), "TASK_TYPE_FORBIDDEN");
    }

    @ExceptionHandler(TaskTypeValidationException.class)
    public ProblemDetail handleTaskTypeValidation(TaskTypeValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid task type request", ex.getMessage(), "TASK_TYPE_VALIDATION_FAILED");
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
        return problemDetail;
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        problemDetail.setProperty("code", code);
        return problemDetail;
    }
}

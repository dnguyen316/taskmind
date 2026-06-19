package com.taskmind.backend;

import com.taskmind.backend.integration.infrastructure.ProviderClientException;
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
}

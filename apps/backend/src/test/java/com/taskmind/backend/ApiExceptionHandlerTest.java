package com.taskmind.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskmind.backend.config.logging.ProblemDetailLogging;
import com.taskmind.backend.config.logging.RequestContextFilter;
import com.taskmind.backend.config.logging.RequestLoggingProperties;
import com.taskmind.backend.integration.infrastructure.ProviderClientException;
import com.taskmind.backend.task.application.TaskValidationException;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class ApiExceptionHandlerTest {
    private static final String CORRELATION_ID = "test-correlation-123";

    private final ApiExceptionHandler handler = new ApiExceptionHandler(problemDetailLogging());
    private final RequestContextFilter filter = new RequestContextFilter(new RequestLoggingProperties(null, null, false));

    @Test
    void enrichesTaskProblemsWithCorrelationIdWithoutChangingCode() throws Exception {
        ProblemDetail problem = handleRequestWithCorrelationId(
                () -> handler.handleTaskValidation(new TaskValidationException("Invalid priority")));

        assertThat(problem.getProperties())
                .containsEntry("code", "TASK_VALIDATION_FAILED")
                .containsEntry("correlationId", CORRELATION_ID);
    }

    @Test
    void enrichesOptimisticLockProblemsWithCorrelationId() throws Exception {
        ProblemDetail problem = handleRequestWithCorrelationId(
                () -> handler.handleOptimisticLockingFailure(
                        new ObjectOptimisticLockingFailureException(String.class, "resource-id")));

        assertThat(problem.getProperties()).containsEntry("correlationId", CORRELATION_ID);
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void enrichesProviderClientProblemsWithCorrelationIdWithoutChangingCode() throws Exception {
        ProblemDetail problem = handleRequestWithCorrelationId(
                () -> handler.handleProviderClientException(new ProviderClientException(
                        HttpStatus.BAD_GATEWAY, "PROVIDER_UNAVAILABLE", "GitHub request failed", true)));

        assertThat(problem.getProperties())
                .containsEntry("code", "PROVIDER_UNAVAILABLE")
                .containsEntry("providerStatus", HttpStatus.BAD_GATEWAY.value())
                .containsEntry("retrySafe", true)
                .containsEntry("correlationId", CORRELATION_ID);
    }

    private ProblemDetail handleRequestWithCorrelationId(ProblemSupplier supplier)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", CORRELATION_ID);
        MockHttpServletResponse response = new MockHttpServletResponse();
        ProblemDetail[] problem = new ProblemDetail[1];

        filter.doFilter(
                request, response, (servletRequest, servletResponse) -> problem[0] = supplier.get());

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(CORRELATION_ID);
        return problem[0];
    }

    private static ObjectProvider<ProblemDetailLogging> problemDetailLogging() {
        return new ObjectProvider<>() {
            @Override
            public ProblemDetailLogging getObject(Object... args) {
                return getObject();
            }

            @Override
            public ProblemDetailLogging getObject() {
                return new ProblemDetailLogging();
            }

            @Override
            public ProblemDetailLogging getIfAvailable() {
                return getObject();
            }

            @Override
            public ProblemDetailLogging getIfUnique() {
                return getObject();
            }

            @Override
            public Stream<ProblemDetailLogging> stream() {
                return Stream.of(getObject());
            }
        };
    }

    @FunctionalInterface
    private interface ProblemSupplier {
        ProblemDetail get();
    }
}

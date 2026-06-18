package com.taskmind.ai.config.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailLogging {
    private static final Logger log = LoggerFactory.getLogger(ProblemDetailLogging.class);

    public ProblemDetail enrich(ProblemDetail problemDetail) {
        String correlationId = RequestCorrelation.currentId();
        if (correlationId != null) {
            problemDetail.setProperty("correlationId", correlationId);
        }
        log.debug(
                "problem_detail status={} title={} correlationId={}",
                problemDetail.getStatus(),
                problemDetail.getTitle(),
                correlationId);
        return problemDetail;
    }
}

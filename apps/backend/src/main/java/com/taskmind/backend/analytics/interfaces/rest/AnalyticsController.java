package com.taskmind.backend.analytics.interfaces.rest;

import com.taskmind.backend.analytics.application.*;
import com.taskmind.backend.auth.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/reports")
public class AnalyticsController {
    private final AnalyticsApplicationService service;

    public AnalyticsController(AnalyticsApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ReportsResponse reports(
            AuthenticatedUser requester, @RequestParam(defaultValue = "week") String range) {
        return service.reports(requester, ReportsRange.from(range));
    }

    @ExceptionHandler(InvalidReportsRangeException.class)
    public ProblemDetail invalidReportsRange(InvalidReportsRangeException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid reports range");
        problem.setDetail("Reports range must be one of: " + String.join(", ", ex.allowedValues()));
        problem.setProperty("allowedValues", ex.allowedValues());
        problem.setProperty("invalidValue", ex.value());
        return problem;
    }
}

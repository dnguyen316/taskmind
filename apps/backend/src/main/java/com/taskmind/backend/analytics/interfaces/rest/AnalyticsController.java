package com.taskmind.backend.analytics.interfaces.rest;

import com.taskmind.backend.analytics.application.*;
import com.taskmind.backend.auth.AuthenticatedUser;
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
}

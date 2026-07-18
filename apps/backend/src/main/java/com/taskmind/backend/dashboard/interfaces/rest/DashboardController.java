package com.taskmind.backend.dashboard.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.dashboard.application.*;
import com.taskmind.backend.relay.RelayClientException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/dashboard")
public class DashboardController {
    private final DashboardApplicationService service;

    public DashboardController(DashboardApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardResponse dashboard(AuthenticatedUser requester) {
        try {
            return service.dashboard(requester);
        } catch (RelayClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "A dependent service is temporarily unavailable.", e);
        }
    }
}

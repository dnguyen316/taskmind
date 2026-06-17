package com.taskmind.backend.team.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.team.application.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/team")
public class TeamController {
    private final TeamApplicationService service;

    public TeamController(TeamApplicationService service) {
        this.service = service;
    }

    @GetMapping("/directory")
    public TeamDirectoryResponse directory(AuthenticatedUser requester) {
        try {
            return service.directory(requester);
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }
}

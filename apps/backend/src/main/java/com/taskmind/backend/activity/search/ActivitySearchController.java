package com.taskmind.backend.activity.search;

import com.taskmind.backend.auth.AuthenticatedUser;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/activity/search")
@Validated
public class ActivitySearchController {
    private final ActivitySearchApplicationService service;

    public ActivitySearchController(ActivitySearchApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ActivitySearchDocument> search(
            AuthenticatedUser requester,
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        try {
            return service.search(requester, query, size);
        } catch (ActivitySearchDisabledException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), e);
        }
    }

    @GetMapping("/suggest")
    public List<String> suggest(
            AuthenticatedUser requester,
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        try {
            return service.suggest(requester, query, size);
        } catch (ActivitySearchDisabledException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), e);
        }
    }
}

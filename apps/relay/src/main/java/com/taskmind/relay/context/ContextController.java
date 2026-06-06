package com.taskmind.relay.context;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/context")
public class ContextController {
    private final ContextQueryService contextQueryService;

    public ContextController(ContextQueryService contextQueryService) {
        this.contextQueryService = contextQueryService;
    }

    @GetMapping("/users/{userId}/tasks")
    public List<Map<String, Object>> userTasks(@PathVariable UUID userId) {
        return contextQueryService.userTasks(userId);
    }

    @GetMapping("/projects/{projectId}/tasks")
    public List<Map<String, Object>> projectTasks(@PathVariable UUID projectId) {
        return contextQueryService.projectTasks(projectId);
    }

    @GetMapping("/projects/{projectId}/metrics")
    public List<Map<String, Object>> projectMetrics(@PathVariable UUID projectId) {
        return contextQueryService.projectMetrics(projectId);
    }
}

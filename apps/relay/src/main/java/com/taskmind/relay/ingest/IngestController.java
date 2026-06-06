package com.taskmind.relay.ingest;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/events")
public class IngestController {
    private final IngestApplicationService ingestApplicationService;

    public IngestController(IngestApplicationService ingestApplicationService) {
        this.ingestApplicationService = ingestApplicationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Boolean>> ingest(@RequestBody String body) {
        return ResponseEntity.ok(Map.of("ingested", ingestApplicationService.ingest(body)));
    }
}

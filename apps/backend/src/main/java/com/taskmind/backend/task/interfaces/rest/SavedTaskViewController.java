package com.taskmind.backend.task.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.task.application.SavedTaskViewService;
import com.taskmind.backend.task.interfaces.rest.dto.SavedTaskViewRequest;
import com.taskmind.backend.task.interfaces.rest.dto.SavedTaskViewResponse;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/task-saved-views")
public class SavedTaskViewController {
    private final SavedTaskViewService service;
    private final ObjectMapper mapper;
    public SavedTaskViewController(SavedTaskViewService service, ObjectMapper mapper){this.service=service;this.mapper=mapper;}
    @GetMapping
    public List<SavedTaskViewResponse> list(AuthenticatedUser user){return service.list(user).stream().map(SavedTaskViewResponse::from).toList();}
    @PostMapping
    public ResponseEntity<SavedTaskViewResponse> create(AuthenticatedUser user, @Valid @RequestBody SavedTaskViewRequest request) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(SavedTaskViewResponse.from(service.create(user, request.name(), mapper.writeValueAsString(request.filters()==null?Map.of():request.filters()))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(AuthenticatedUser user, @PathVariable UUID id){return service.delete(user,id)?ResponseEntity.noContent().build():ResponseEntity.notFound().build();}
}

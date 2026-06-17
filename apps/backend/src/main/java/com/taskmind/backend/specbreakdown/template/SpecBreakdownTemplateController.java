package com.taskmind.backend.specbreakdown.template;
import java.time.Instant; import java.util.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController
public class SpecBreakdownTemplateController { private final Map<UUID, Template> templates=new java.util.concurrent.ConcurrentHashMap<>();
 @PostMapping("/v1/projects/{projectId}/spec-templates") public ResponseEntity<Template> create(@PathVariable UUID projectId,@RequestBody TemplateRequest r){ Template t=new Template(UUID.randomUUID(),projectId,r.name(),r.description(),r.fields()==null?"{}":r.fields(),Instant.now(),Instant.now()); templates.put(t.id(),t); return ResponseEntity.status(HttpStatus.CREATED).body(t);} 
 @GetMapping("/v1/projects/{projectId}/spec-templates") public List<Template> list(@PathVariable UUID projectId){return templates.values().stream().filter(t->t.projectId().equals(projectId)).toList();}
 @PutMapping("/v1/spec-templates/{id}") public Template update(@PathVariable UUID id,@RequestBody TemplateRequest r){ Template old=templates.get(id); Template t=new Template(id,old.projectId(),r.name(),r.description(),r.fields()==null?old.fields():r.fields(),old.createdAt(),Instant.now()); templates.put(id,t); return t;}
 @DeleteMapping("/v1/spec-templates/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id){templates.remove(id); return ResponseEntity.noContent().build();}
 public record Template(UUID id,UUID projectId,String name,String description,String fields,Instant createdAt,Instant updatedAt){} public record TemplateRequest(String name,String description,String fields){}
}

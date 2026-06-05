package com.taskmind.backend.project.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.project.application.*;
import com.taskmind.backend.project.domain.model.Project;
import com.taskmind.backend.project.interfaces.rest.dto.*;
import com.taskmind.backend.security.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/v1/projects") @Validated
public class ProjectController {
 private final ProjectApplicationService projects; private final ProjectMembershipApplicationService memberships; private final AuthenticatedUserResolver users;
 public ProjectController(ProjectApplicationService p,ProjectMembershipApplicationService m,AuthenticatedUserResolver u){projects=p;memberships=m;users=u;}
 @PostMapping public ResponseEntity<Project> createProject(@Valid @RequestBody CreateProjectRequest request,Authentication auth,@RequestHeader(value="X-User-Id",required=false)UUID uid,@RequestHeader(value="X-User-Roles",required=false)String roles){try{var actor=resolve(auth,uid,roles,request.ownerUserId());var owner=actor.isPrivileged()?request.ownerUserId():actor.userId();return ResponseEntity.status(HttpStatus.CREATED).body(projects.create(new CreateProjectCommand(request.name(),request.key(),request.description(),owner)));}catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.CONFLICT,e.getMessage(),e);}}
 @GetMapping public List<Project> listProjects(@RequestParam(defaultValue="false")boolean includeArchived,Authentication auth,@RequestHeader(value="X-User-Id",required=false)UUID uid,@RequestHeader(value="X-User-Roles",required=false)String roles){var actor=resolve(auth,uid,roles,null);return projects.list(includeArchived).stream().filter(p->canRead(actor,p)).toList();}
 @GetMapping("/{id}") public ResponseEntity<Project> getProject(@PathVariable UUID id,Authentication auth,@RequestHeader(value="X-User-Id",required=false)UUID uid,@RequestHeader(value="X-User-Roles",required=false)String roles){var actor=resolve(auth,uid,roles,null);return projects.findById(id).filter(p->canRead(actor,p)).map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());}
 @PatchMapping("/{id}") public ResponseEntity<Project> updateProject(@PathVariable UUID id,@Valid @RequestBody UpdateProjectRequest request,Authentication auth,@RequestHeader(value="X-User-Id",required=false)UUID uid,@RequestHeader(value="X-User-Roles",required=false)String roles){authorizeOwner(resolve(auth,uid,roles,null),id);try{return projects.update(id,new UpdateProjectCommand(request.name(),request.key(),request.description())).map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());}catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.CONFLICT,e.getMessage(),e);}}
 @PatchMapping("/{id}/archive") public ResponseEntity<Project> archiveProject(@PathVariable UUID id,Authentication auth,@RequestHeader(value="X-User-Id",required=false)UUID uid,@RequestHeader(value="X-User-Roles",required=false)String roles){authorizeOwner(resolve(auth,uid,roles,null),id);return projects.archive(new ArchiveProjectCommand(id)).map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());}
 private AuthenticatedUser resolve(Authentication auth,UUID uid,String roles,UUID fallback){return users.resolve(auth,uid==null?fallback:uid,roles);} private boolean canRead(AuthenticatedUser a,Project p){return a.isPrivileged()||p.ownerUserId().equals(a.userId())||memberships.isMember(p.id(),a.userId());} private void authorizeOwner(AuthenticatedUser a,UUID id){var p=projects.findById(id).orElseThrow();if(!a.isPrivileged()&&!p.ownerUserId().equals(a.userId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the project owner can mutate the project");}
}

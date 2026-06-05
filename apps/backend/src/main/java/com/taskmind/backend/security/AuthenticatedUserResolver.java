package com.taskmind.backend.security;
import com.taskmind.backend.auth.AuthenticatedUser; import java.util.*; import org.springframework.security.core.Authentication; import org.springframework.stereotype.Component;
@Component public class AuthenticatedUserResolver {
 public AuthenticatedUser resolve(Authentication authentication, UUID fallbackUserId, String fallbackRoles){
  if(authentication!=null&&authentication.isAuthenticated()){var roles=authentication.getAuthorities().stream().map(a->a.getAuthority().replaceFirst("^ROLE_","")).collect(java.util.stream.Collectors.toSet());return new AuthenticatedUser(UUID.fromString(authentication.getName()),roles);}
  if(fallbackUserId==null)throw new IllegalArgumentException("Authenticated user is required"); var roles=fallbackRoles==null?Set.<String>of():Arrays.stream(fallbackRoles.split(",")).map(String::trim).filter(s->!s.isBlank()).collect(java.util.stream.Collectors.toSet());return new AuthenticatedUser(fallbackUserId,roles);
 }
}

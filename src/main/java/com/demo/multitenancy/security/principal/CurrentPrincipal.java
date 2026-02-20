package com.demo.multitenancy.security.principal;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentPrincipal {

  public Optional<PrincipalInfo> get() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return Optional.empty();
    }

    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String subject = jwt.getSubject();
      String email = jwt.getClaimAsString("email");
      return Optional.of(new PrincipalInfo(subject, email));
    }

    return Optional.of(new PrincipalInfo(authentication.getName(), null));
  }

  public PrincipalInfo require() {
    return get().orElseThrow(() -> new IllegalStateException("No authenticated principal"));
  }
}

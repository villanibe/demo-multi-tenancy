package com.demo.multitenancy.tenant;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class RequestTenantIdResolver implements TenantIdResolver {
  private final TenantProperties properties;

  public RequestTenantIdResolver(TenantProperties properties) {
    this.properties = properties;
  }

  @Override
  public Optional<String> resolveTenantId(HttpServletRequest request) {
    String fromHeader = request.getHeader(properties.getHeader());
    if (fromHeader != null && !fromHeader.isBlank()) {
      return Optional.of(fromHeader.trim());
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      Object claim = jwt.getClaims().get(properties.getClaim());
      if (claim instanceof String s && !s.isBlank()) {
        return Optional.of(s.trim());
      }
    }

    return Optional.empty();
  }
}

package com.demo.multitenancy.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RequestTenantIdResolverTest {

  @Test
  void resolveTenantId_givenHeader_thenUsesHeader() {
    TenantProperties properties = new TenantProperties();
    properties.setHeader("X-Tenant-Id");

    RequestTenantIdResolver resolver = new RequestTenantIdResolver(properties);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Tenant-Id", "tenant-a");

    assertThat(resolver.resolveTenantId(request)).contains("tenant-a");
  }

  @Test
  void resolveTenantId_givenJwtClaim_thenUsesClaim() {
    TenantProperties properties = new TenantProperties();
    properties.setClaim("tenant_id");

    RequestTenantIdResolver resolver = new RequestTenantIdResolver(properties);

    Jwt jwt = new Jwt(
        "token",
        Instant.now(),
        Instant.now().plusSeconds(60),
        Map.of("alg", "none"),
        Map.of("sub", "user1", "tenant_id", "tenant-b"));

    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

    try {
      MockHttpServletRequest request = new MockHttpServletRequest();
      assertThat(resolver.resolveTenantId(request)).contains("tenant-b");
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}

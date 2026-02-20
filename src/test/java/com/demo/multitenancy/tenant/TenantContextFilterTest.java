package com.demo.multitenancy.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TenantContextFilterTest {

  @Test
  void doFilterInternal_givenTenantResolved_thenSetsAndClearsContext() throws ServletException, IOException {
    TenantIdResolver resolver = (request) -> Optional.of("tenant-x");
    TenantContextFilter filter = new TenantContextFilter(resolver);

    MockHttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse res = new MockHttpServletResponse();

    FilterChain chain = new FilterChain() {
      @Override
      public void doFilter(ServletRequest request, ServletResponse response) {
        assertThat(TenantContext.getTenantId()).contains("tenant-x");
      }
    };

    filter.doFilter(req, res, chain);

    assertThat(TenantContext.getTenantId()).isEmpty();
  }
}

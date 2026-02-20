package com.demo.multitenancy.tenant;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

public class TenantContextFilter extends OncePerRequestFilter {
  private final TenantIdResolver tenantIdResolver;

  public TenantContextFilter(TenantIdResolver tenantIdResolver) {
    this.tenantIdResolver = tenantIdResolver;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      tenantIdResolver.resolveTenantId(request).ifPresent(TenantContext::setTenantId);
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }
}

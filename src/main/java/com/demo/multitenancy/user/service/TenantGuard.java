package com.demo.multitenancy.user.service;

import com.demo.multitenancy.tenant.TenantContext;

import org.springframework.stereotype.Component;

@Component
public class TenantGuard {
  public String requireTenantId() {
    return TenantContext.getTenantId().orElseThrow(() -> new IllegalStateException("Tenant context not set"));
  }
}

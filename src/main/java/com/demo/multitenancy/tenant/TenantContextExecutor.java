package com.demo.multitenancy.tenant;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

@Component
public class TenantContextExecutor {

  public <T> T runWithTenant(String tenantId, Supplier<T> supplier) {
    String previous = TenantContext.getTenantId().orElse(null);

    try {
      TenantContext.setTenantId(tenantId);
      return supplier.get();
    } finally {
      if (previous == null) {
        TenantContext.clear();
      } else {
        TenantContext.setTenantId(previous);
      }
    }
  }
}

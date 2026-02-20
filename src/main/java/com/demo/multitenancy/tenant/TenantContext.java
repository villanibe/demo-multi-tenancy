package com.demo.multitenancy.tenant;

import java.util.Optional;

public final class TenantContext {
  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

  private TenantContext() {
  }

  public static Optional<String> getTenantId() {
    return Optional.ofNullable(CURRENT.get());
  }

  public static void setTenantId(String tenantId) {
    CURRENT.set(tenantId);
  }

  public static void clear() {
    CURRENT.remove();
  }
}

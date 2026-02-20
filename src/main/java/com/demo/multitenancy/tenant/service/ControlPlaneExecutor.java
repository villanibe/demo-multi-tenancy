package com.demo.multitenancy.tenant.service;

import java.util.Optional;
import java.util.function.Supplier;

import com.demo.multitenancy.tenant.TenantContext;

import org.springframework.stereotype.Component;

@Component
public class ControlPlaneExecutor {

  public <T> T run(Supplier<T> supplier) {
    Optional<String> previous = TenantContext.getTenantId();
    try {
      TenantContext.clear();
      return supplier.get();
    } finally {
      previous.ifPresent(TenantContext::setTenantId);
    }
  }

  public void run(Runnable runnable) {
    run(() -> {
      runnable.run();
      return null;
    });
  }
}

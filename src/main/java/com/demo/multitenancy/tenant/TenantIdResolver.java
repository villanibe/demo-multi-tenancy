package com.demo.multitenancy.tenant;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

public interface TenantIdResolver {
  Optional<String> resolveTenantId(HttpServletRequest request);
}

package com.demo.multitenancy.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

public class SpringCurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {
  private final TenantProperties properties;

  public SpringCurrentTenantIdentifierResolver(TenantProperties properties) {
    this.properties = properties;
  }

  @Override
  public String resolveCurrentTenantIdentifier() {
    return TenantContext.getTenantId().orElse(properties.getDefaultTenant());
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }
}

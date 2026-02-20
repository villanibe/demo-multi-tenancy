package com.demo.multitenancy.tenant.connection;

import java.sql.Connection;
import java.sql.SQLException;

public class NoopTenantConnectionCustomizer implements TenantConnectionCustomizer {
  @Override
  public void customize(Connection connection, String tenantId) throws SQLException {
    // no-op
  }
}

package com.demo.multitenancy.tenant.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class PostgresRlsTenantConnectionCustomizer implements TenantConnectionCustomizer {
  private final String settingName;

  public PostgresRlsTenantConnectionCustomizer(String settingName) {
    this.settingName = settingName;
  }

  @Override
  public void customize(Connection connection, String tenantId) throws SQLException {
    if (tenantId == null || tenantId.isBlank()) {
      return;
    }

    // Postgres: SET <setting> = '<tenant>'
    // For stricter scoping consider `SET LOCAL` inside a transaction.
    try (Statement statement = connection.createStatement()) {
      statement.execute("SET " + settingName + " = '" + tenantId.replace("'", "''") + "'");
    }
  }
}

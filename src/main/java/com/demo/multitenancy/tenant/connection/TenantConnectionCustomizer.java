package com.demo.multitenancy.tenant.connection;

import java.sql.Connection;
import java.sql.SQLException;

public interface TenantConnectionCustomizer {
  void customize(Connection connection, String tenantId) throws SQLException;
}

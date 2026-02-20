package com.demo.multitenancy.tenant.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

public class DatabasePerTenantConnectionProvider implements MultiTenantConnectionProvider<String> {
  private final DataSource anyDataSource;
  private final Map<String, DataSource> tenantDataSources;
  private final TenantConnectionCustomizer customizer;

  public DatabasePerTenantConnectionProvider(
      DataSource anyDataSource,
      Map<String, DataSource> tenantDataSources,
      TenantConnectionCustomizer customizer) {
    this.anyDataSource = anyDataSource;
    this.tenantDataSources = tenantDataSources;
    this.customizer = customizer;
  }

  @Override
  public Connection getAnyConnection() throws SQLException {
    return anyDataSource.getConnection();
  }

  @Override
  public void releaseAnyConnection(Connection connection) throws SQLException {
    connection.close();
  }

  @Override
  public Connection getConnection(String tenantIdentifier) throws SQLException {
    DataSource tenantDataSource = tenantDataSources.get(tenantIdentifier);
    if (tenantDataSource == null) {
      throw new SQLException("No DataSource configured for tenant: " + tenantIdentifier);
    }
    Connection connection = tenantDataSource.getConnection();
    customizer.customize(connection, tenantIdentifier);
    return connection;
  }

  @Override
  public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
    connection.close();
  }

  @Override
  public boolean supportsAggressiveRelease() {
    return false;
  }

  @Override
  public boolean isUnwrappableAs(Class<?> unwrapType) {
    return false;
  }

  @Override
  public <T> T unwrap(Class<T> unwrapType) {
    throw new UnsupportedOperationException("unwrap not supported");
  }
}

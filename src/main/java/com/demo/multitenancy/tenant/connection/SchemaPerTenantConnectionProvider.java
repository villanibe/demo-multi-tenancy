package com.demo.multitenancy.tenant.connection;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

public class SchemaPerTenantConnectionProvider implements MultiTenantConnectionProvider<String> {
  private final DataSource dataSource;
  private final String schemaPrefix;
  private final TenantConnectionCustomizer customizer;

  public SchemaPerTenantConnectionProvider(DataSource dataSource, String schemaPrefix, TenantConnectionCustomizer customizer) {
    this.dataSource = dataSource;
    this.schemaPrefix = schemaPrefix;
    this.customizer = customizer;
  }

  @Override
  public Connection getAnyConnection() throws SQLException {
    return dataSource.getConnection();
  }

  @Override
  public void releaseAnyConnection(Connection connection) throws SQLException {
    connection.close();
  }

  @Override
  public Connection getConnection(String tenantIdentifier) throws SQLException {
    Connection connection = getAnyConnection();
    String schema = schemaPrefix + tenantIdentifier;
    connection.setSchema(schema);
    customizer.customize(connection, tenantIdentifier);
    return connection;
  }

  @Override
  public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
    try {
      connection.setSchema("public");
    } catch (SQLException ignored) {
      // best-effort
    }
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

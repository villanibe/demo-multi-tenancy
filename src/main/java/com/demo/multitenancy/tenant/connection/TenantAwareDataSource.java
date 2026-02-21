package com.demo.multitenancy.tenant.connection;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.jdbc.datasource.AbstractDataSource;

public class TenantAwareDataSource extends AbstractDataSource {

  private final DataSource delegate;
  private final CurrentTenantIdentifierResolver<String> tenantIdentifierResolver;
  private final TenantConnectionCustomizer customizer;

  public TenantAwareDataSource(
      DataSource delegate,
      CurrentTenantIdentifierResolver<String> tenantIdentifierResolver,
      TenantConnectionCustomizer customizer) {
    this.delegate = delegate;
    this.tenantIdentifierResolver = tenantIdentifierResolver;
    this.customizer = customizer;
  }

  @Override
  public Connection getConnection() throws SQLException {
    Connection connection = delegate.getConnection();
    customize(connection);
    return connection;
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    Connection connection = delegate.getConnection(username, password);
    customize(connection);
    return connection;
  }

  private void customize(Connection connection) throws SQLException {
    String tenantId = tenantIdentifierResolver.resolveCurrentTenantIdentifier();
    customizer.customize(connection, tenantId);
  }
}

package com.demo.multitenancy.tenant.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.Statement;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class SchemaPerTenantConnectionProviderTest {

  @Test
  void setsSchemaPerTenantAndResetsOnRelease() throws Exception {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:schema_mode_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
    dataSource.setUser("sa");
    dataSource.setPassword("");

    try (Connection connection = dataSource.getConnection(); Statement st = connection.createStatement()) {
      st.execute("CREATE SCHEMA IF NOT EXISTS tenant_tenant_a");
    }

    TenantConnectionCustomizer customizer = mock(TenantConnectionCustomizer.class);
    SchemaPerTenantConnectionProvider provider = new SchemaPerTenantConnectionProvider(dataSource, "tenant_", customizer);

    Connection tenantConnection = provider.getConnection("tenant_a");
    try {
      assertThat(tenantConnection.getSchema()).isNotBlank();
      assertThat(tenantConnection.getSchema().toLowerCase()).isEqualTo("tenant_tenant_a");
      verify(customizer).customize(tenantConnection, "tenant_a");
    } finally {
      provider.releaseConnection("tenant_a", tenantConnection);
    }

    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection.getSchema()).isNotBlank();
    }
  }
}

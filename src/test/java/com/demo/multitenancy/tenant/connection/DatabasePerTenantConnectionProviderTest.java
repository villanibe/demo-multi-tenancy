package com.demo.multitenancy.tenant.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.util.Map;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class DatabasePerTenantConnectionProviderTest {

  @Test
  void routesConnectionsToPerTenantDataSource() throws Exception {
    DataSource anyDataSource = newH2("jdbc:h2:mem:any_db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
    DataSource tenantA = newH2("jdbc:h2:mem:tenant_a_db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
    DataSource tenantB = newH2("jdbc:h2:mem:tenant_b_db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");

    TenantConnectionCustomizer customizer = mock(TenantConnectionCustomizer.class);
    DatabasePerTenantConnectionProvider provider = new DatabasePerTenantConnectionProvider(
        anyDataSource,
        Map.of("tenant-a", tenantA, "tenant-b", tenantB),
        customizer);

    try (Connection connectionA = provider.getConnection("tenant-a")) {
      assertThat(connectionA.getMetaData().getURL()).contains("tenant_a_db");
      verify(customizer).customize(connectionA, "tenant-a");
    }

    try (Connection connectionB = provider.getConnection("tenant-b")) {
      assertThat(connectionB.getMetaData().getURL()).contains("tenant_b_db");
      verify(customizer).customize(connectionB, "tenant-b");
    }
  }

  @Test
  void throwsWhenNoTenantDataSourceConfigured() {
    DataSource anyDataSource = newH2("jdbc:h2:mem:any_db_missing;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
    TenantConnectionCustomizer customizer = mock(TenantConnectionCustomizer.class);
    DatabasePerTenantConnectionProvider provider = new DatabasePerTenantConnectionProvider(anyDataSource, Map.of(), customizer);

    assertThatThrownBy(() -> {
      try (Connection ignored = provider.getConnection("missing")) {
        // no-op
      }
    }).hasMessageContaining("No DataSource configured for tenant");
  }

  private static DataSource newH2(String jdbcUrl) {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL(jdbcUrl);
    dataSource.setUser("sa");
    dataSource.setPassword("");
    return dataSource;
  }
}

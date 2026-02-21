package com.demo.multitenancy.tenant.rls;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.demo.multitenancy.tenant.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@SuppressWarnings("resource")
class PostgresRlsEnforcementTest {

  @Container
  @SuppressWarnings("resource")
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("app")
      .withUsername("postgres")
      .withPassword("postgres")
      .withInitScript("db/rls-test-init.sql");

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", () -> "app_user");
    registry.add("spring.datasource.password", () -> "app_user");

    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

    registry.add("app.tenancy.mode", () -> "COLUMN");
    registry.add("app.tenancy.rls.enabled", () -> "true");
    registry.add("app.tenancy.rls.postgres.settingName", () -> "app.tenant_id");
  }

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void enableRlsOnTodoLists() {
    TenantContext.clear();

    jdbcTemplate.execute("ALTER TABLE todo_lists ENABLE ROW LEVEL SECURITY");
    jdbcTemplate.execute("ALTER TABLE todo_lists FORCE ROW LEVEL SECURITY");

    jdbcTemplate.execute("DROP POLICY IF EXISTS tenant_isolation ON todo_lists");
    jdbcTemplate.execute(
        "CREATE POLICY tenant_isolation ON todo_lists " +
            "USING (tenant_id = current_setting('app.tenant_id')) " +
            "WITH CHECK (tenant_id = current_setting('app.tenant_id'))");
  }

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void rlsPreventsCrossTenantReadsWithoutWhereClause() {
    UUID listA = UUID.randomUUID();
    UUID listB = UUID.randomUUID();

    insertTodoList("tenant-a", listA, "List A");
    insertTodoList("tenant-b", listB, "List B");

    TenantContext.setTenantId("tenant-a");
    Integer countA = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM todo_lists", Integer.class);
    assertThat(countA).isEqualTo(1);

    TenantContext.setTenantId("tenant-b");
    Integer countB = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM todo_lists", Integer.class);
    assertThat(countB).isEqualTo(1);

    TenantContext.setTenantId("tenant-a");
    String onlyNameA = jdbcTemplate.queryForObject("SELECT name FROM todo_lists", String.class);
    assertThat(onlyNameA).isEqualTo("List A");

    TenantContext.setTenantId("tenant-b");
    String onlyNameB = jdbcTemplate.queryForObject("SELECT name FROM todo_lists", String.class);
    assertThat(onlyNameB).isEqualTo("List B");
  }

  private void insertTodoList(String tenantId, UUID id, String name) {
    TenantContext.setTenantId(tenantId);

    jdbcTemplate.update(connection -> {
      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO todo_lists (id, tenant_id, name, created_at) VALUES (?, ?, ?, ?)");
      statement.setObject(1, id);
      statement.setString(2, tenantId);
      statement.setString(3, name);
      statement.setObject(4, OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC),
          Types.TIMESTAMP_WITH_TIMEZONE);
      return statement;
    });
  }
}

package com.demo.multitenancy.tenant;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tenancy")
public class TenantProperties {
  private TenantMode mode = TenantMode.COLUMN;
  private String header = "X-Tenant-Id";
  private String claim = "tenant_id";
  private String defaultTenant = "public";

  private final Schema schema = new Schema();
  private final Database database = new Database();
  private final Rls rls = new Rls();

  public TenantMode getMode() {
    return mode;
  }

  public void setMode(TenantMode mode) {
    this.mode = mode;
  }

  public String getHeader() {
    return header;
  }

  public void setHeader(String header) {
    this.header = header;
  }

  public String getClaim() {
    return claim;
  }

  public void setClaim(String claim) {
    this.claim = claim;
  }

  public String getDefaultTenant() {
    return defaultTenant;
  }

  public void setDefaultTenant(String defaultTenant) {
    this.defaultTenant = defaultTenant;
  }

  public Schema getSchema() {
    return schema;
  }

  public Database getDatabase() {
    return database;
  }

  public Rls getRls() {
    return rls;
  }

  public static class Schema {
    private String prefix = "tenant_";

    public String getPrefix() {
      return prefix;
    }

    public void setPrefix(String prefix) {
      this.prefix = prefix;
    }
  }

  public static class Database {
    private final Map<String, DataSourceConfig> tenants = new HashMap<>();

    public Map<String, DataSourceConfig> getTenants() {
      return tenants;
    }

    public static class DataSourceConfig {
      private String jdbcUrl;
      private String username;
      private String password;

      public String getJdbcUrl() {
        return jdbcUrl;
      }

      public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
      }

      public String getUsername() {
        return username;
      }

      public void setUsername(String username) {
        this.username = username;
      }

      public String getPassword() {
        return password;
      }

      public void setPassword(String password) {
        this.password = password;
      }
    }
  }

  public static class Rls {
    private boolean enabled = false;
    private final Postgres postgres = new Postgres();

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Postgres getPostgres() {
      return postgres;
    }

    public static class Postgres {
      private String settingName = "app.tenant_id";

      public String getSettingName() {
        return settingName;
      }

      public void setSettingName(String settingName) {
        this.settingName = settingName;
      }
    }
  }
}

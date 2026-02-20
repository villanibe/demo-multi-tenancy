package com.demo.multitenancy.tenant;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.demo.multitenancy.tenant.connection.DatabasePerTenantConnectionProvider;
import com.demo.multitenancy.tenant.connection.NoopTenantConnectionCustomizer;
import com.demo.multitenancy.tenant.connection.PostgresRlsTenantConnectionCustomizer;
import com.demo.multitenancy.tenant.connection.SchemaPerTenantConnectionProvider;
import com.demo.multitenancy.tenant.connection.TenantConnectionCustomizer;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.hibernate.cfg.MultiTenancySettings;

@Configuration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantConfiguration {

  @Bean
  public CurrentTenantIdentifierResolver<String> currentTenantIdentifierResolver(TenantProperties properties) {
    return new SpringCurrentTenantIdentifierResolver(properties);
  }

  @Bean
  public TenantConnectionCustomizer tenantConnectionCustomizer(TenantProperties properties) {
    if (!properties.getRls().isEnabled()) {
      return new NoopTenantConnectionCustomizer();
    }
    return new PostgresRlsTenantConnectionCustomizer(properties.getRls().getPostgres().getSettingName());
  }

  @Bean
  @ConditionalOnProperty(prefix = "app.tenancy", name = "mode", havingValue = "SCHEMA")
  public MultiTenantConnectionProvider<String> schemaConnectionProvider(
      DataSource dataSource,
      TenantProperties properties,
      TenantConnectionCustomizer customizer) {
    return new SchemaPerTenantConnectionProvider(dataSource, properties.getSchema().getPrefix(), customizer);
  }

  @Bean
  @ConditionalOnProperty(prefix = "app.tenancy", name = "mode", havingValue = "DATABASE")
  public MultiTenantConnectionProvider<String> databaseConnectionProvider(
      DataSource dataSource,
      TenantProperties properties,
      TenantConnectionCustomizer customizer) {

    Map<String, DataSource> tenantDataSources = new HashMap<>();
    for (Map.Entry<String, TenantProperties.Database.DataSourceConfig> entry : properties.getDatabase().getTenants().entrySet()) {
      String tenantId = entry.getKey();
      TenantProperties.Database.DataSourceConfig cfg = entry.getValue();
      DataSource tenantDs = DataSourceBuilder.create()
          .url(cfg.getJdbcUrl())
          .username(cfg.getUsername())
          .password(cfg.getPassword())
          .build();
      tenantDataSources.put(tenantId, tenantDs);
    }

    return new DatabasePerTenantConnectionProvider(dataSource, tenantDataSources, customizer);
  }

  @Bean
  public HibernatePropertiesCustomizer hibernateMultiTenancyCustomizer(
      TenantProperties properties,
      CurrentTenantIdentifierResolver<String> resolver,
      org.springframework.context.ApplicationContext applicationContext) {

    return (hibernateProperties) -> {
      TenantMode mode = properties.getMode();

      if (mode == TenantMode.COLUMN) {
        hibernateProperties.put("hibernate.multiTenancy", "DISCRIMINATOR");
      } else if (mode == TenantMode.SCHEMA) {
        hibernateProperties.put("hibernate.multiTenancy", "SCHEMA");
        MultiTenantConnectionProvider<String> provider = applicationContext.getBean(MultiTenantConnectionProvider.class);
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER, provider);
      } else if (mode == TenantMode.DATABASE) {
        hibernateProperties.put("hibernate.multiTenancy", "DATABASE");
        MultiTenantConnectionProvider<String> provider = applicationContext.getBean(MultiTenantConnectionProvider.class);
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER, provider);
      }

      hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    };
  }
}

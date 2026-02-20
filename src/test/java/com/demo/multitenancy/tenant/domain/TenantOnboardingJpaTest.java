package com.demo.multitenancy.tenant.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.demo.multitenancy.tenant.TenantConfiguration;
import com.demo.multitenancy.tenant.TenantContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@Import(TenantConfiguration.class)
class TenantOnboardingJpaTest {

  @Autowired
  private TenantRegistrationRepository tenantRegistrationRepository;

  @Test
  void tenantRegistration_isStoredInControlPlaneDefaultTenant() {
    try {
      TenantContext.setTenantId("tenant-a");
      tenantRegistrationRepository.save(new TenantRegistration("t-acme", "Acme", Instant.parse("2026-02-20T00:00:00Z")));
      tenantRegistrationRepository.flush();

      TenantContext.clear();
      assertThat(tenantRegistrationRepository.findByTenantId("t-acme")).isPresent();
    } finally {
      TenantContext.clear();
    }
  }
}

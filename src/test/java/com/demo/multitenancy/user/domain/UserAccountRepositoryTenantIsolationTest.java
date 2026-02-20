package com.demo.multitenancy.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

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
class UserAccountRepositoryTenantIsolationTest {

  @Autowired
  private UserAccountRepository repository;

  @Test
  void findByEmail_givenSameEmailDifferentTenants_thenIsolationWorks() {
    try {
      TenantContext.setTenantId("tenant-a");
      repository.save(new UserAccount("same@demo.com", "Alice"));
      repository.flush();

      TenantContext.setTenantId("tenant-b");
      repository.save(new UserAccount("same@demo.com", "Bob"));
      repository.flush();

      TenantContext.setTenantId("tenant-a");
      UserAccount a = repository.findByTenantIdAndEmail("tenant-a", "same@demo.com").orElseThrow();
      assertThat(a.getDisplayName()).isEqualTo("Alice");
      assertThat(a.getTenantId()).isEqualTo("tenant-a");

      TenantContext.setTenantId("tenant-b");
      UserAccount b = repository.findByTenantIdAndEmail("tenant-b", "same@demo.com").orElseThrow();
      assertThat(b.getDisplayName()).isEqualTo("Bob");
      assertThat(b.getTenantId()).isEqualTo("tenant-b");
    } finally {
      TenantContext.clear();
    }
  }
}

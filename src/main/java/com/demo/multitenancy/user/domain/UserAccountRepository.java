package com.demo.multitenancy.user.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
  Optional<UserAccount> findByTenantIdAndEmail(String tenantId, String email);

  Optional<UserAccount> findByTenantIdAndId(String tenantId, UUID id);
}

package com.demo.multitenancy.tenant.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingTenantSignupRepository extends JpaRepository<PendingTenantSignup, UUID> {
  Optional<PendingTenantSignup> findByProviderAndProviderSessionId(String provider, String providerSessionId);
}

package com.demo.multitenancy.billing.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, UUID> {
  Optional<CheckoutSession> findByTenantIdAndProviderAndIdempotencyKey(String tenantId, String provider, String idempotencyKey);

  Optional<CheckoutSession> findByProviderAndProviderSessionId(String provider, String providerSessionId);
}

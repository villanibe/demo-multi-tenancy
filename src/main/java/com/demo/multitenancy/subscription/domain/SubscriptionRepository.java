package com.demo.multitenancy.subscription.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
  Optional<Subscription> findFirstByTenantIdOrderByIdAsc(String tenantId);
}

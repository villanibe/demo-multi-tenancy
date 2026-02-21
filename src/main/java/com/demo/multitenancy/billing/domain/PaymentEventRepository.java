package com.demo.multitenancy.billing.domain;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {
  boolean existsByProviderAndEventId(String provider, String eventId);
}

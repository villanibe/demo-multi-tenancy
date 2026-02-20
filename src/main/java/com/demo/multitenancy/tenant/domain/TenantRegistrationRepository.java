package com.demo.multitenancy.tenant.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRegistrationRepository extends JpaRepository<TenantRegistration, UUID> {
  Optional<TenantRegistration> findByTenantId(String tenantId);
}

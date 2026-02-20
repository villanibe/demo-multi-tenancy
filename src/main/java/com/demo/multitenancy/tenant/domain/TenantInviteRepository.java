package com.demo.multitenancy.tenant.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantInviteRepository extends JpaRepository<TenantInvite, UUID> {
  Optional<TenantInvite> findByToken(String token);
}

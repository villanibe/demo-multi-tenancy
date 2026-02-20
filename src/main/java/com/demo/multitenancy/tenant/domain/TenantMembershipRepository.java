package com.demo.multitenancy.tenant.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {
  Optional<TenantMembership> findByTenantIdAndSubject(String tenantId, String subject);

  List<TenantMembership> findAllBySubjectOrderByCreatedAtAsc(String subject);
}

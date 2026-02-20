package com.demo.multitenancy.user.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
  Optional<Permission> findByTenantIdAndCode(String tenantId, String code);

  List<Permission> findAllByTenantId(String tenantId);
}

package com.demo.multitenancy.user.domain;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface RoleRepository extends JpaRepository<Role, UUID> {
  Optional<Role> findByTenantIdAndName(String tenantId, String name);

  @EntityGraph(attributePaths = "permissions")
  Optional<Role> findWithPermissionsByTenantIdAndName(String tenantId, String name);

  @EntityGraph(attributePaths = "permissions")
  List<Role> findAllWithPermissionsByTenantId(String tenantId);
}

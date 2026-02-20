package com.demo.multitenancy.user.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.demo.multitenancy.user.domain.Permission;
import com.demo.multitenancy.user.domain.PermissionRepository;
import com.demo.multitenancy.user.domain.Role;
import com.demo.multitenancy.user.domain.RoleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationAccessService {

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final TenantGuard tenantGuard;

  public OrganizationAccessService(RoleRepository roleRepository, PermissionRepository permissionRepository, TenantGuard tenantGuard) {
    this.roleRepository = roleRepository;
    this.permissionRepository = permissionRepository;
    this.tenantGuard = tenantGuard;
  }

  @Transactional(readOnly = true)
  public List<Role> listRolesWithPermissions() {
    String tenantId = tenantGuard.requireTenantId();
    List<Role> roles = roleRepository.findAllWithPermissionsByTenantId(tenantId);
    roles.sort(Comparator.comparing(Role::getName));
    return roles;
  }

  @Transactional(readOnly = true)
  public List<Permission> listPermissions() {
    String tenantId = tenantGuard.requireTenantId();
    List<Permission> permissions = permissionRepository.findAllByTenantId(tenantId);
    permissions.sort(Comparator.comparing(Permission::getCode));
    return permissions;
  }

  @Transactional
  public Role createRole(String name) {
    String tenantId = tenantGuard.requireTenantId();
    String normalized = normalizeRoleName(name);

    roleRepository.findByTenantIdAndName(tenantId, normalized).ifPresent(existing -> {
      throw new IllegalArgumentException("Role already exists: " + normalized);
    });

    return roleRepository.save(new Role(normalized));
  }

  @Transactional
  public Role grantPermission(String roleName, String permissionCode) {
    String tenantId = tenantGuard.requireTenantId();
    String normalizedRoleName = normalizeRoleName(roleName);
    String normalizedPermissionCode = normalizePermissionCode(permissionCode);

    Role role = roleRepository.findWithPermissionsByTenantIdAndName(tenantId, normalizedRoleName)
        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + normalizedRoleName));

    Permission permission = permissionRepository.findByTenantIdAndCode(tenantId, normalizedPermissionCode)
        .orElseGet(() -> permissionRepository.save(new Permission(normalizedPermissionCode)));

    role.getPermissions().add(permission);
    return roleRepository.save(role);
  }

  @Transactional
  public Role revokePermission(String roleName, String permissionCode) {
    String tenantId = tenantGuard.requireTenantId();
    String normalizedRoleName = normalizeRoleName(roleName);
    String normalizedPermissionCode = normalizePermissionCode(permissionCode);

    Role role = roleRepository.findWithPermissionsByTenantIdAndName(tenantId, normalizedRoleName)
        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + normalizedRoleName));

    role.getPermissions().removeIf(p -> normalizedPermissionCode.equals(p.getCode()));
    return roleRepository.save(role);
  }

  private static String normalizeRoleName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Role name is required");
    }

    String normalized = name.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Role name is required");
    }

    return normalized.toUpperCase(Locale.ROOT);
  }

  private static String normalizePermissionCode(String code) {
    if (code == null) {
      throw new IllegalArgumentException("Permission code is required");
    }

    String normalized = code.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Permission code is required");
    }

    return normalized;
  }
}

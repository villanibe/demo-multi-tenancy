package com.demo.multitenancy.user.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.demo.multitenancy.security.authorization.PermissionCodes;
import com.demo.multitenancy.tenant.TenantContextExecutor;
import com.demo.multitenancy.user.domain.Permission;
import com.demo.multitenancy.user.domain.PermissionRepository;
import com.demo.multitenancy.user.domain.Role;
import com.demo.multitenancy.user.domain.RoleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantAuthorizationBootstrapper {

  private final TenantContextExecutor tenantContextExecutor;
  private final PermissionRepository permissionRepository;
  private final RoleRepository roleRepository;

  public TenantAuthorizationBootstrapper(
      TenantContextExecutor tenantContextExecutor,
      PermissionRepository permissionRepository,
      RoleRepository roleRepository) {
    this.tenantContextExecutor = tenantContextExecutor;
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
  }

  @Transactional
  public void bootstrapIfMissing(String tenantId) {
    tenantContextExecutor.runWithTenant(tenantId, () -> {
      Map<String, List<String>> rolesToPermissions = defaultRolePermissions();

      for (String code : allPermissionCodes()) {
        permissionRepository.findByTenantIdAndCode(tenantId, code)
            .orElseGet(() -> permissionRepository.save(new Permission(code)));
      }

      for (Map.Entry<String, List<String>> entry : rolesToPermissions.entrySet()) {
        String roleName = entry.getKey();
        List<String> permissionCodes = entry.getValue();

        Role role = roleRepository.findWithPermissionsByTenantIdAndName(tenantId, roleName)
            .orElseGet(() -> roleRepository.save(new Role(roleName)));

        boolean changed = false;
        for (String permissionCode : permissionCodes) {
          Permission permission = permissionRepository.findByTenantIdAndCode(tenantId, permissionCode)
              .orElseGet(() -> permissionRepository.save(new Permission(permissionCode)));

          if (role.getPermissions().add(permission)) {
            changed = true;
          }
        }

        if (changed) {
          roleRepository.save(role);
        }
      }

      return null;
    });
  }

  private static List<String> allPermissionCodes() {
    return List.of(
        PermissionCodes.TODO_READ,
        PermissionCodes.TODO_WRITE,
        PermissionCodes.USER_READ,
        PermissionCodes.USER_WRITE,
        PermissionCodes.SUBSCRIPTION_READ,
        PermissionCodes.SUBSCRIPTION_WRITE,
        PermissionCodes.BILLING_WRITE,
        PermissionCodes.TENANT_INVITE_WRITE,
        PermissionCodes.AUTHZ_READ,
        PermissionCodes.AUTHZ_WRITE
    );
  }

  private static Map<String, List<String>> defaultRolePermissions() {
    Map<String, List<String>> map = new LinkedHashMap<>();

    map.put("OWNER", allPermissionCodes());

    map.put("ADMIN", List.of(
        PermissionCodes.TODO_READ,
        PermissionCodes.TODO_WRITE,
        PermissionCodes.USER_READ,
        PermissionCodes.USER_WRITE,
        PermissionCodes.SUBSCRIPTION_READ,
        PermissionCodes.SUBSCRIPTION_WRITE,
        PermissionCodes.BILLING_WRITE,
      PermissionCodes.TENANT_INVITE_WRITE,
      PermissionCodes.AUTHZ_READ,
      PermissionCodes.AUTHZ_WRITE
    ));

    map.put("MEMBER", List.of(
        PermissionCodes.TODO_READ,
        PermissionCodes.TODO_WRITE,
        PermissionCodes.USER_READ,
        PermissionCodes.SUBSCRIPTION_READ
    ));

    return map;
  }
}

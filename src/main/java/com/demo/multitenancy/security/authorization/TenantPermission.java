package com.demo.multitenancy.security.authorization;

import com.demo.multitenancy.security.principal.CurrentPrincipal;
import com.demo.multitenancy.security.principal.PrincipalInfo;
import com.demo.multitenancy.tenant.TenantContextExecutor;
import com.demo.multitenancy.tenant.domain.TenantMembership;
import com.demo.multitenancy.tenant.domain.TenantMembershipRepository;
import com.demo.multitenancy.tenant.service.ControlPlaneExecutor;
import com.demo.multitenancy.user.domain.Role;
import com.demo.multitenancy.user.domain.RoleRepository;
import com.demo.multitenancy.user.service.TenantGuard;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("tenantPermission")
public class TenantPermission {
  private static final String SCOPE_PREFIX = "SCOPE_";
  private static final String ROLE_ADMIN = "ROLE_ADMIN";

  private final CurrentPrincipal currentPrincipal;
  private final TenantMembershipRepository membershipRepository;
  private final ControlPlaneExecutor controlPlane;
  private final TenantContextExecutor tenantContextExecutor;
  private final RoleRepository roleRepository;
  private final TenantGuard tenantGuard;

  public TenantPermission(
      CurrentPrincipal currentPrincipal,
      TenantMembershipRepository membershipRepository,
      ControlPlaneExecutor controlPlane,
      TenantContextExecutor tenantContextExecutor,
      RoleRepository roleRepository,
      TenantGuard tenantGuard) {
    this.currentPrincipal = currentPrincipal;
    this.membershipRepository = membershipRepository;
    this.controlPlane = controlPlane;
    this.tenantContextExecutor = tenantContextExecutor;
    this.roleRepository = roleRepository;
    this.tenantGuard = tenantGuard;
  }

  public boolean has(String permissionCode) {
    String tenantId = tenantGuard.requireTenantId();
    return hasForTenant(tenantId, permissionCode);
  }

  public boolean hasForTenant(String tenantId, String permissionCode) {
    if (hasAuthority(SCOPE_PREFIX + PermissionCodes.ADMIN) || hasAuthority(ROLE_ADMIN)) {
      return true;
    }

    if (permissionCode == null || permissionCode.isBlank()) {
      return false;
    }

    if (hasAuthority(SCOPE_PREFIX + permissionCode)) {
      return true;
    }

    PrincipalInfo principal = currentPrincipal.require();
    TenantMembership membership = controlPlane.run(() -> membershipRepository.findByTenantIdAndSubject(tenantId, principal.getSubject())
        .orElse(null));

    if (membership == null) {
      return false;
    }

    return tenantContextExecutor.runWithTenant(tenantId, () -> roleAllows(tenantId, membership.getRole().name(), permissionCode));
  }

  private boolean roleAllows(String tenantId, String roleName, String permissionCode) {
    Role role = roleRepository.findWithPermissionsByTenantIdAndName(tenantId, roleName).orElse(null);
    if (role == null) {
      return false;
    }

    return role.getPermissions().stream().anyMatch(p -> permissionCode.equals(p.getCode()));
  }

  private static boolean hasAuthority(String authority) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }

    for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
      if (authority.equals(grantedAuthority.getAuthority())) {
        return true;
      }
    }

    return false;
  }
}

package com.demo.multitenancy.user.api;

import java.util.List;

import com.demo.multitenancy.user.domain.Permission;
import com.demo.multitenancy.user.domain.Role;
import com.demo.multitenancy.user.service.OrganizationAccessService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organization-access")
public class OrganizationAccessController {

  private final OrganizationAccessService organizationAccessService;

  public OrganizationAccessController(OrganizationAccessService organizationAccessService) {
    this.organizationAccessService = organizationAccessService;
  }

  @Operation(summary = "List tenant roles")
  @PreAuthorize("@tenantPermission.has('authz.read')")
  @GetMapping("/roles")
  public ResponseEntity<List<RoleResponse>> listRoles() {
    List<RoleResponse> response = organizationAccessService.listRolesWithPermissions().stream().map(OrganizationAccessController::toRoleResponse).toList();
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Create a tenant role")
  @PreAuthorize("@tenantPermission.has('authz.write')")
  @PostMapping("/roles")
  public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
    Role created = organizationAccessService.createRole(request.getName());
    return ResponseEntity.ok(toRoleResponse(created));
  }

  @Operation(summary = "Grant a permission to a role")
  @PreAuthorize("@tenantPermission.has('authz.write')")
  @PutMapping("/roles/{roleName}/permissions/{permissionCode:.+}")
  public ResponseEntity<RoleResponse> grantPermission(
      @PathVariable("roleName") String roleName,
      @PathVariable("permissionCode") String permissionCode) {
    Role updated = organizationAccessService.grantPermission(roleName, permissionCode);
    return ResponseEntity.ok(toRoleResponse(updated));
  }

  @Operation(summary = "Revoke a permission from a role")
  @PreAuthorize("@tenantPermission.has('authz.write')")
  @DeleteMapping("/roles/{roleName}/permissions/{permissionCode:.+}")
  public ResponseEntity<RoleResponse> revokePermission(
      @PathVariable("roleName") String roleName,
      @PathVariable("permissionCode") String permissionCode) {
    Role updated = organizationAccessService.revokePermission(roleName, permissionCode);
    return ResponseEntity.ok(toRoleResponse(updated));
  }

  @Operation(summary = "List tenant permissions")
  @PreAuthorize("@tenantPermission.has('authz.read')")
  @GetMapping("/permissions")
  public ResponseEntity<List<PermissionResponse>> listPermissions() {
    List<PermissionResponse> response = organizationAccessService.listPermissions().stream().map(OrganizationAccessController::toPermissionResponse).toList();
    return ResponseEntity.ok(response);
  }

  private static RoleResponse toRoleResponse(Role role) {
    RoleResponse response = new RoleResponse();
    response.setName(role.getName());
    response.setPermissionCodes(role.getPermissions().stream().map(Permission::getCode).sorted().toList());
    return response;
  }

  private static PermissionResponse toPermissionResponse(Permission permission) {
    PermissionResponse response = new PermissionResponse();
    response.setCode(permission.getCode());
    return response;
  }
}

package com.demo.multitenancy.user.domain;

import java.util.HashSet;
import java.util.Set;

import com.demo.multitenancy.shared.jpa.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "roles",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_roles_tenant_name", columnNames = { "tenant_id", "name" })
    })
public class Role extends TenantScopedEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @ManyToMany
  @JoinTable(name = "role_permissions")
  private Set<Permission> permissions = new HashSet<>();

  protected Role() {
  }

  public Role(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Set<Permission> getPermissions() {
    return permissions;
  }
}

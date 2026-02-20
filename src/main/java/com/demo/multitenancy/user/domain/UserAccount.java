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
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_tenant_email", columnNames = { "tenant_id", "email" })
    })
public class UserAccount extends TenantScopedEntity {

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @ManyToMany
  @JoinTable(name = "user_roles")
  private Set<Role> roles = new HashSet<>();

  protected UserAccount() {
  }

  public UserAccount(String email, String displayName) {
    this.email = email;
    this.displayName = displayName;
  }

  public String getEmail() {
    return email;
  }

  public String getDisplayName() {
    return displayName;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Set<Role> getRoles() {
    return roles;
  }

  public void disable() {
    this.enabled = false;
  }
}

package com.demo.multitenancy.user.domain;

import com.demo.multitenancy.shared.jpa.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "permissions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_permissions_tenant_code", columnNames = { "tenant_id", "code" })
    })
public class Permission extends TenantScopedEntity {

  @Column(name = "code", nullable = false)
  private String code;

  protected Permission() {
  }

  public Permission(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}

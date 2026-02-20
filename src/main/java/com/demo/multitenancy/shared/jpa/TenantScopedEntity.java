package com.demo.multitenancy.shared.jpa;

import java.util.UUID;

import com.demo.multitenancy.tenant.TenantContext;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import org.hibernate.annotations.UuidGenerator;

@MappedSuperclass
public abstract class TenantScopedEntity {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private String tenantId;

  public UUID getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  @PrePersist
  void assignTenantIdIfMissing() {
    if (tenantId != null && !tenantId.isBlank()) {
      return;
    }
    this.tenantId = TenantContext.getTenantId()
        .orElseThrow(() -> new IllegalStateException("Tenant context not set for tenant-scoped entity"));
  }
}

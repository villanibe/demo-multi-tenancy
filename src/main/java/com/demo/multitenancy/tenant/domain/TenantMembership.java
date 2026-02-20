package com.demo.multitenancy.tenant.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
    name = "tenant_memberships",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_membership_tenant_subject", columnNames = { "tenant_id", "subject" })
    })
public class TenantMembership {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private String tenantId;

  @Column(name = "subject", nullable = false, updatable = false)
  private String subject;

  @Column(name = "email")
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private TenantRole role;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected TenantMembership() {
  }

  public TenantMembership(String tenantId, String subject, String email, TenantRole role, Instant createdAt) {
    this.tenantId = tenantId;
    this.subject = subject;
    this.email = email;
    this.role = role;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getSubject() {
    return subject;
  }

  public String getEmail() {
    return email;
  }

  public TenantRole getRole() {
    return role;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

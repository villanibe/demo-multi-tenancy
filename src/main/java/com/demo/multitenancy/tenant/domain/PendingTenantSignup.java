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
    name = "pending_tenant_signups",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pending_signup_provider_session", columnNames = { "provider", "provider_session_id" })
    })
public class PendingTenantSignup {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "provider", nullable = false)
  private String provider;

  @Column(name = "provider_session_id", nullable = false)
  private String providerSessionId;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "tenant_display_name", nullable = false)
  private String tenantDisplayName;

  @Column(name = "owner_subject", nullable = false)
  private String ownerSubject;

  @Column(name = "owner_email")
  private String ownerEmail;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private PendingTenantSignupStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  protected PendingTenantSignup() {
  }

  public PendingTenantSignup(
      String provider,
      String providerSessionId,
      String tenantId,
      String tenantDisplayName,
      String ownerSubject,
      String ownerEmail,
      Instant createdAt) {
    this.provider = provider;
    this.providerSessionId = providerSessionId;
    this.tenantId = tenantId;
    this.tenantDisplayName = tenantDisplayName;
    this.ownerSubject = ownerSubject;
    this.ownerEmail = ownerEmail;
    this.status = PendingTenantSignupStatus.PENDING_PAYMENT;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderSessionId() {
    return providerSessionId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getTenantDisplayName() {
    return tenantDisplayName;
  }

  public String getOwnerSubject() {
    return ownerSubject;
  }

  public String getOwnerEmail() {
    return ownerEmail;
  }

  public PendingTenantSignupStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void markCompleted(Instant now) {
    this.status = PendingTenantSignupStatus.COMPLETED;
    this.completedAt = now;
  }
}

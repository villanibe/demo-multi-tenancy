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
    name = "tenant_invites",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_invites_token", columnNames = { "token" })
    })
public class TenantInvite {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private String tenantId;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "token", nullable = false, updatable = false)
  private String token;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private TenantInviteStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private TenantRole role;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "accepted_at")
  private Instant acceptedAt;

  protected TenantInvite() {
  }

  public TenantInvite(String tenantId, String email, String token, TenantRole role, Instant createdAt, Instant expiresAt) {
    this.tenantId = tenantId;
    this.email = email;
    this.token = token;
    this.role = role != null ? role : TenantRole.MEMBER;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
    this.status = TenantInviteStatus.PENDING;
  }

  public UUID getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getEmail() {
    return email;
  }

  public String getToken() {
    return token;
  }

  public TenantInviteStatus getStatus() {
    return status;
  }

  public TenantRole getRole() {
    return role;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getAcceptedAt() {
    return acceptedAt;
  }

  public boolean isExpired(Instant now) {
    return now.isAfter(expiresAt);
  }

  public void accept(Instant now) {
    this.status = TenantInviteStatus.ACCEPTED;
    this.acceptedAt = now;
  }

  public void expire() {
    this.status = TenantInviteStatus.EXPIRED;
  }

  public void revoke() {
    this.status = TenantInviteStatus.REVOKED;
  }
}

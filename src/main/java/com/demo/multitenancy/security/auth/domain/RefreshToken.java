package com.demo.multitenancy.security.auth.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "ix_refresh_tokens_hash", columnList = "token_hash", unique = true),
        @Index(name = "ix_refresh_tokens_subject", columnList = "subject")
    })
public class RefreshToken {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "subject", nullable = false, updatable = false)
  private String subject;

  // Hex-encoded SHA-256 (64 chars)
  @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
  private String tokenHash;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "replaced_by_id")
  private UUID replacedById;

  protected RefreshToken() {
  }

  public RefreshToken(String subject, String tokenHash, Instant createdAt, Instant expiresAt) {
    this.subject = subject;
    this.tokenHash = tokenHash;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public UUID getId() {
    return id;
  }

  public String getSubject() {
    return subject;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public UUID getReplacedById() {
    return replacedById;
  }

  public boolean isExpired(Instant now) {
    return expiresAt.isBefore(now) || expiresAt.equals(now);
  }

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public boolean isActive(Instant now) {
    return !isRevoked() && !isExpired(now);
  }

  public void revoke(Instant now, UUID replacedById) {
    this.revokedAt = now;
    this.replacedById = replacedById;
  }

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}

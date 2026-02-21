package com.demo.multitenancy.security.auth.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
    name = "identities",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_identities_email", columnNames = { "email" }),
        @UniqueConstraint(name = "uk_identities_subject", columnNames = { "subject" })
    })
public class Identity {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "subject", nullable = false, updatable = false)
  private String subject;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Identity() {
  }

  public Identity(String subject, String email, String passwordHash, Instant now) {
    this.subject = subject;
    this.email = email;
    this.passwordHash = passwordHash;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getSubject() {
    return subject;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setPasswordHash(String passwordHash, Instant now) {
    this.passwordHash = passwordHash;
    this.updatedAt = now;
  }

  public void disable(Instant now) {
    this.enabled = false;
    this.updatedAt = now;
  }

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
    if (updatedAt == null) {
      updatedAt = createdAt;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }
}

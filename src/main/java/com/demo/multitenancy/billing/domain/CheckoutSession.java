package com.demo.multitenancy.billing.domain;

import java.time.Instant;
import java.util.UUID;

import com.demo.multitenancy.subscription.domain.BillingInterval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
    name = "checkout_sessions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_checkout_provider_session", columnNames = { "provider", "provider_session_id" }),
        @UniqueConstraint(name = "uk_checkout_tenant_provider_idempotency", columnNames = { "tenant_id", "provider", "idempotency_key" })
    })
public class CheckoutSession {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "provider", nullable = false)
  private String provider;

  @Column(name = "provider_session_id", nullable = false)
  private String providerSessionId;

  @Column(name = "checkout_url", nullable = false, length = 2000)
  private String checkoutUrl;

  @Column(name = "idempotency_key")
  private String idempotencyKey;

  @Column(name = "plan_code", nullable = false)
  private String planCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "billing_interval", nullable = false)
  private BillingInterval interval;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private CheckoutSessionStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  protected CheckoutSession() {
  }

  public CheckoutSession(
      String tenantId,
      String provider,
      String providerSessionId,
      String checkoutUrl,
      String idempotencyKey,
      String planCode,
      BillingInterval interval,
      CheckoutSessionStatus status,
      Instant createdAt) {
    this.tenantId = tenantId;
    this.provider = provider;
    this.providerSessionId = providerSessionId;
    this.checkoutUrl = checkoutUrl;
    this.idempotencyKey = idempotencyKey;
    this.planCode = planCode;
    this.interval = interval;
    this.status = status;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderSessionId() {
    return providerSessionId;
  }

  public String getCheckoutUrl() {
    return checkoutUrl;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public String getPlanCode() {
    return planCode;
  }

  public BillingInterval getInterval() {
    return interval;
  }

  public CheckoutSessionStatus getStatus() {
    return status;
  }

  public void markCompleted(Instant completedAt) {
    this.status = CheckoutSessionStatus.COMPLETED;
    this.completedAt = completedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }
}

package com.demo.multitenancy.subscription.domain;

import java.time.Instant;

import com.demo.multitenancy.shared.jpa.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscriptions")
public class Subscription extends TenantScopedEntity {

  @Column(name = "plan_code", nullable = false)
  private String planCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SubscriptionStatus status;

  @Column(name = "trial_ends_at")
  private Instant trialEndsAt;

  @Column(name = "current_period_ends_at")
  private Instant currentPeriodEndsAt;

  protected Subscription() {
  }

  public Subscription(String planCode, SubscriptionStatus status, Instant trialEndsAt, Instant currentPeriodEndsAt) {
    this.planCode = planCode;
    this.status = status;
    this.trialEndsAt = trialEndsAt;
    this.currentPeriodEndsAt = currentPeriodEndsAt;
  }

  public String getPlanCode() {
    return planCode;
  }

  public SubscriptionStatus getStatus() {
    return status;
  }

  public Instant getTrialEndsAt() {
    return trialEndsAt;
  }

  public Instant getCurrentPeriodEndsAt() {
    return currentPeriodEndsAt;
  }
}

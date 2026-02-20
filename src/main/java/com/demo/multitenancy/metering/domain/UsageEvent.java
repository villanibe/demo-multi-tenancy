package com.demo.multitenancy.metering.domain;

import java.time.Instant;

import com.demo.multitenancy.shared.jpa.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "usage_events")
public class UsageEvent extends TenantScopedEntity {

  @Column(name = "metric_key", nullable = false)
  private String metricKey;

  @Column(name = "quantity", nullable = false)
  private long quantity;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;

  protected UsageEvent() {
  }

  public UsageEvent(String metricKey, long quantity, Instant recordedAt) {
    this.metricKey = metricKey;
    this.quantity = quantity;
    this.recordedAt = recordedAt;
  }

  public String getMetricKey() {
    return metricKey;
  }

  public long getQuantity() {
    return quantity;
  }

  public Instant getRecordedAt() {
    return recordedAt;
  }
}

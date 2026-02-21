package com.demo.multitenancy.billing.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
    name = "payment_events",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_events_provider_event", columnNames = { "provider", "event_id" })
    })
public class PaymentEvent {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "provider", nullable = false)
  private String provider;

  @Column(name = "tenant_id")
  private String tenantId;

  @Column(name = "event_id", nullable = false)
  private String eventId;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  protected PaymentEvent() {
  }

  public PaymentEvent(String provider, String tenantId, String eventId, Instant receivedAt) {
    this.provider = provider;
    this.tenantId = tenantId;
    this.eventId = eventId;
    this.receivedAt = receivedAt;
  }

  public UUID getId() {
    return id;
  }

  public String getProvider() {
    return provider;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getEventId() {
    return eventId;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }
}

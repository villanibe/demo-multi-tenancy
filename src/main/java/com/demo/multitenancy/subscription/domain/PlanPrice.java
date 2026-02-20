package com.demo.multitenancy.subscription.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
    name = "plan_prices",
    uniqueConstraints = {
    @UniqueConstraint(name = "uk_plan_prices_plan_interval", columnNames = { "plan_id", "billing_interval" })
    })
public class PlanPrice {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "plan_id", nullable = false)
  private Plan plan;

  @Enumerated(EnumType.STRING)
  @Column(name = "billing_interval", nullable = false)
  private BillingInterval interval;

  @Column(name = "currency", nullable = false)
  private String currency;

  @Column(name = "unit_amount_cents", nullable = false)
  private long unitAmountCents;

  @ElementCollection
  @CollectionTable(name = "plan_price_provider_prices", joinColumns = @JoinColumn(name = "plan_price_id"))
  @MapKeyColumn(name = "provider", nullable = false)
  @Column(name = "provider_price_id", nullable = false)
  private Map<String, String> providerPriceIds = new HashMap<>();

  protected PlanPrice() {
  }

  public PlanPrice(Plan plan, BillingInterval interval, String currency, long unitAmountCents) {
    this.plan = plan;
    this.interval = interval;
    this.currency = currency;
    this.unitAmountCents = unitAmountCents;
  }

  public UUID getId() {
    return id;
  }

  public Plan getPlan() {
    return plan;
  }

  public BillingInterval getInterval() {
    return interval;
  }

  public String getCurrency() {
    return currency;
  }

  public long getUnitAmountCents() {
    return unitAmountCents;
  }

  public Map<String, String> getProviderPriceIds() {
    return providerPriceIds;
  }
}

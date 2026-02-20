package com.demo.multitenancy.subscription.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
    name = "plans",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_plans_code", columnNames = { "code" })
    })
public class Plan {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "code", nullable = false)
  private String code;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "trial_days", nullable = false)
  private int trialDays;

  @ElementCollection
  @CollectionTable(name = "plan_entitlements", joinColumns = @JoinColumn(name = "plan_id"))
  @Column(name = "entitlement_code", nullable = false)
  private Set<String> planFeatureCodes = new HashSet<>();

  protected Plan() {
  }

  public Plan(String code, String name, int trialDays) {
    this.code = code;
    this.name = name;
    this.trialDays = trialDays;
  }

  public UUID getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public int getTrialDays() {
    return trialDays;
  }

  public Set<String> getPlanFeatureCodes() {
    return planFeatureCodes;
  }
}

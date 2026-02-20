package com.demo.multitenancy.subscription.planfeature;

import java.time.Clock;
import java.time.Instant;

import com.demo.multitenancy.subscription.domain.Plan;
import com.demo.multitenancy.subscription.domain.PlanRepository;
import com.demo.multitenancy.subscription.domain.Subscription;
import com.demo.multitenancy.subscription.domain.SubscriptionRepository;
import com.demo.multitenancy.subscription.domain.SubscriptionStatus;
import com.demo.multitenancy.user.service.TenantGuard;

import org.springframework.stereotype.Component;

@Component("tenantPlanFeature")
public class TenantPlanFeature {

  private final SubscriptionRepository subscriptionRepository;
  private final PlanRepository planRepository;
  private final TenantGuard tenantGuard;
  private final Clock clock;

  public TenantPlanFeature(
      SubscriptionRepository subscriptionRepository,
      PlanRepository planRepository,
      TenantGuard tenantGuard,
      Clock clock) {
    this.subscriptionRepository = subscriptionRepository;
    this.planRepository = planRepository;
    this.tenantGuard = tenantGuard;
    this.clock = clock;
  }

  public boolean has(String featureCode) {
    if (featureCode == null || featureCode.isBlank()) {
      return false;
    }

    String tenantId = tenantGuard.requireTenantId();

    Subscription subscription = subscriptionRepository.findFirstByTenantIdOrderByIdAsc(tenantId).orElse(null);
    if (subscription == null) {
      return false;
    }

    if (!isSubscriptionCurrentlyEntitled(subscription)) {
      return false;
    }

    Plan plan = planRepository.findByCode(subscription.getPlanCode()).orElse(null);
    if (plan == null) {
      return false;
    }

    return plan.getPlanFeatureCodes().contains(featureCode);
  }

  private boolean isSubscriptionCurrentlyEntitled(Subscription subscription) {
    Instant now = Instant.now(clock);

    if (subscription.getStatus() == SubscriptionStatus.TRIALING) {
      return subscription.getTrialEndsAt() != null && subscription.getTrialEndsAt().isAfter(now);
    }

    if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
      return subscription.getCurrentPeriodEndsAt() != null && subscription.getCurrentPeriodEndsAt().isAfter(now);
    }

    return false;
  }
}

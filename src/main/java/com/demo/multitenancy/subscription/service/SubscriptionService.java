package com.demo.multitenancy.subscription.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.demo.multitenancy.subscription.domain.Plan;
import com.demo.multitenancy.subscription.domain.PlanRepository;
import com.demo.multitenancy.subscription.domain.Subscription;
import com.demo.multitenancy.subscription.domain.SubscriptionRepository;
import com.demo.multitenancy.subscription.domain.SubscriptionStatus;
import com.demo.multitenancy.subscription.domain.BillingInterval;
import com.demo.multitenancy.user.service.TenantGuard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {
  private final PlanRepository planRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final TenantGuard tenantGuard;
  private final Clock clock;

  public SubscriptionService(
      PlanRepository planRepository,
      SubscriptionRepository subscriptionRepository,
      TenantGuard tenantGuard,
      Clock clock) {
    this.planRepository = planRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.tenantGuard = tenantGuard;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public Subscription getCurrentSubscription() {
    String tenantId = tenantGuard.requireTenantId();
    return subscriptionRepository.findFirstByTenantIdOrderByIdAsc(tenantId)
        .orElseThrow(() -> new IllegalStateException("No subscription found for tenant"));
  }

  @Transactional
  public Subscription startTrialIfMissing(String planCode) {
    String tenantId = tenantGuard.requireTenantId();

    return subscriptionRepository.findFirstByTenantIdOrderByIdAsc(tenantId)
        .orElseGet(() -> {
          Plan plan = planRepository.findByCode(planCode)
              .orElseThrow(() -> new IllegalArgumentException("Unknown plan: " + planCode));

          Instant now = Instant.now(clock);
          Instant trialEndsAt = now.plusSeconds((long) plan.getTrialDays() * 24 * 3600);
          Subscription subscription = new Subscription(plan.getCode(), SubscriptionStatus.TRIALING, trialEndsAt, trialEndsAt);
          return subscriptionRepository.save(subscription);
        });
  }

  @Transactional
  public Subscription activatePaid(String planCode, BillingInterval interval) {
    String tenantId = tenantGuard.requireTenantId();

    Plan plan = planRepository.findByCode(planCode)
        .orElseThrow(() -> new IllegalArgumentException("Unknown plan: " + planCode));

    Instant now = Instant.now(clock);
    Instant currentPeriodEndsAt = now.plus(periodDays(interval), ChronoUnit.DAYS);

    Subscription subscription = subscriptionRepository.findFirstByTenantIdOrderByIdAsc(tenantId)
        .orElseGet(() -> subscriptionRepository.save(new Subscription(plan.getCode(), SubscriptionStatus.ACTIVE, null, currentPeriodEndsAt)));

    subscription.activatePaid(plan.getCode(), currentPeriodEndsAt);
    return subscription;
  }

  private static long periodDays(BillingInterval interval) {
    return switch (interval) {
      case MONTHLY -> 30;
      case YEARLY -> 365;
    };
  }
}

package com.demo.multitenancy.subscription.service;

import java.util.Set;

import com.demo.multitenancy.subscription.domain.BillingInterval;
import com.demo.multitenancy.subscription.domain.Plan;
import com.demo.multitenancy.subscription.domain.PlanPrice;
import com.demo.multitenancy.subscription.domain.PlanPriceRepository;
import com.demo.multitenancy.subscription.domain.PlanRepository;
import com.demo.multitenancy.subscription.planfeature.PlanFeatureCodes;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlanCatalogSeeder implements ApplicationRunner {

  private final PlanRepository planRepository;
  private final PlanPriceRepository planPriceRepository;

  public PlanCatalogSeeder(PlanRepository planRepository, PlanPriceRepository planPriceRepository) {
    this.planRepository = planRepository;
    this.planPriceRepository = planPriceRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    seedPlan(
        "starter",
        "Starter",
        7,
        Set.of(PlanFeatureCodes.USERS_MANAGE),
        1900,
        19000);

    seedPlan(
        "pro",
        "Pro",
        14,
        Set.of(PlanFeatureCodes.USERS_MANAGE),
        4900,
        49000);

    seedPlan(
        "enterprise",
        "Enterprise",
        0,
        Set.of(PlanFeatureCodes.USERS_MANAGE),
        19900,
        199000);
  }

  private void seedPlan(
      String code,
      String name,
      int trialDays,
      Set<String> planFeatureCodes,
      long monthlyCents,
      long yearlyCents) {
    Plan plan = planRepository.findByCode(code).orElseGet(() -> planRepository.save(new Plan(code, name, trialDays)));
    plan.getPlanFeatureCodes().addAll(planFeatureCodes);
    planRepository.save(plan);

    seedPrice(plan, BillingInterval.MONTHLY, "USD", monthlyCents, "stripe", "price_" + code + "_monthly");
    seedPrice(plan, BillingInterval.YEARLY, "USD", yearlyCents, "stripe", "price_" + code + "_yearly");
  }

  private void seedPrice(
      Plan plan,
      BillingInterval interval,
      String currency,
      long unitAmountCents,
      String provider,
      String providerPriceId) {
    PlanPrice price = planPriceRepository
        .findByPlan_CodeAndInterval(plan.getCode(), interval)
        .orElseGet(() -> planPriceRepository.save(new PlanPrice(plan, interval, currency, unitAmountCents)));

    price.getProviderPriceIds().putIfAbsent(provider, providerPriceId);
    planPriceRepository.save(price);
  }
}

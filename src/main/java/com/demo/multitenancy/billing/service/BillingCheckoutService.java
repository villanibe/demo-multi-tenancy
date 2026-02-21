package com.demo.multitenancy.billing.service;

import java.time.Clock;
import java.time.Instant;

import com.demo.multitenancy.billing.domain.CheckoutSession;
import com.demo.multitenancy.billing.domain.CheckoutSessionRepository;
import com.demo.multitenancy.billing.domain.CheckoutSessionStatus;
import com.demo.multitenancy.billing.gateway.PaymentGateway;
import com.demo.multitenancy.billing.gateway.PaymentGatewayCheckoutSession;
import com.demo.multitenancy.subscription.domain.BillingInterval;
import com.demo.multitenancy.subscription.domain.PlanPrice;
import com.demo.multitenancy.subscription.domain.PlanPriceRepository;
import com.demo.multitenancy.subscription.domain.PlanRepository;
import com.demo.multitenancy.tenant.service.ControlPlaneExecutor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingCheckoutService {

  private static final String VIRTUAL_PROVIDER = "virtual";

  private final PlanRepository planRepository;
  private final PlanPriceRepository planPriceRepository;
  private final CheckoutSessionRepository checkoutSessionRepository;
  private final ControlPlaneExecutor controlPlane;
  private final Clock clock;

  public BillingCheckoutService(
      PlanRepository planRepository,
      PlanPriceRepository planPriceRepository,
      CheckoutSessionRepository checkoutSessionRepository,
      ControlPlaneExecutor controlPlane,
      Clock clock) {
    this.planRepository = planRepository;
    this.planPriceRepository = planPriceRepository;
    this.checkoutSessionRepository = checkoutSessionRepository;
    this.controlPlane = controlPlane;
    this.clock = clock;
  }

  @Transactional
  public CheckoutSessionResult createCheckout(
      String tenantId,
      PaymentGateway gateway,
      String planCode,
      BillingInterval interval,
      String idempotencyKey) {

    CheckoutSession existing = controlPlane.run(() -> {
      if (idempotencyKey == null || idempotencyKey.isBlank()) {
        return null;
      }
      return checkoutSessionRepository
          .findByTenantIdAndProviderAndIdempotencyKey(tenantId, gateway.provider(), idempotencyKey)
          .orElse(null);
    });

    if (existing != null) {
      // For the blueprint: treat it as idempotent even if already completed.
      return new CheckoutSessionResult(existing.getProviderSessionId(), existing.getCheckoutUrl());
    }

    planRepository.findByCode(planCode)
        .orElseThrow(() -> new IllegalArgumentException("Unknown plan: " + planCode));

    PlanPrice planPrice = planPriceRepository
        .findByPlan_CodeAndInterval(planCode, interval)
        .orElseThrow(() -> new IllegalArgumentException("No price configured for plan " + planCode + " (" + interval + ")"));

    String providerPriceId = planPrice.getProviderPriceIds().get(gateway.provider());
    if (providerPriceId == null || providerPriceId.isBlank()) {
      if (VIRTUAL_PROVIDER.equalsIgnoreCase(gateway.provider())) {
        // Virtual provider is for local E2E flows; it does not require pre-provisioned price ids.
        providerPriceId = "virt_price_" + planCode + "_" + interval.name().toLowerCase();
      } else {
        throw new IllegalArgumentException(
            "No provider price id configured for provider '" + gateway.provider() + "' and plan " + planCode + " (" + interval + ")");
      }
    }

    PaymentGatewayCheckoutSession created = gateway.createCheckoutSession(tenantId, providerPriceId, planCode, interval);
    Instant now = Instant.now(clock);

    controlPlane.run(() -> checkoutSessionRepository.save(new CheckoutSession(
        tenantId,
        gateway.provider(),
        created.getSessionId(),
        created.getUrl(),
        normalize(idempotencyKey),
        planCode,
        interval,
        CheckoutSessionStatus.PENDING,
        now)));

    return new CheckoutSessionResult(created.getSessionId(), created.getUrl());
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public record CheckoutSessionResult(String sessionId, String url) {
  }
}

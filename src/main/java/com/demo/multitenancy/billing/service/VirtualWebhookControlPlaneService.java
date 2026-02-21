package com.demo.multitenancy.billing.service;

import java.time.Clock;
import java.time.Instant;

import com.demo.multitenancy.billing.webhook.virtual.VirtualWebhookRequest;
import com.demo.multitenancy.billing.domain.CheckoutSession;
import com.demo.multitenancy.billing.domain.CheckoutSessionRepository;
import com.demo.multitenancy.billing.domain.CheckoutSessionStatus;
import com.demo.multitenancy.billing.domain.PaymentEvent;
import com.demo.multitenancy.billing.domain.PaymentEventRepository;
import com.demo.multitenancy.subscription.domain.BillingInterval;
import com.demo.multitenancy.tenant.service.ControlPlaneExecutor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VirtualWebhookControlPlaneService {

  public static final String PROVIDER = "virtual";
  public static final String CHECKOUT_COMPLETED = "checkout.session.completed";

  private final CheckoutSessionRepository checkoutSessionRepository;
  private final PaymentEventRepository paymentEventRepository;
  private final ControlPlaneExecutor controlPlane;
  private final Clock clock;

  public VirtualWebhookControlPlaneService(
      CheckoutSessionRepository checkoutSessionRepository,
      PaymentEventRepository paymentEventRepository,
      ControlPlaneExecutor controlPlane,
      Clock clock) {
    this.checkoutSessionRepository = checkoutSessionRepository;
    this.paymentEventRepository = paymentEventRepository;
    this.controlPlane = controlPlane;
    this.clock = clock;
  }

  /**
   * Returns null when the webhook event has already been processed (idempotent replay).
   */
  @Transactional
  public CompletedCheckoutEvent registerAndResolve(VirtualWebhookRequest request) {
    return controlPlane.run(() -> {
      if (!CHECKOUT_COMPLETED.equals(request.getType())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported event type: " + request.getType());
      }

      CheckoutSession session = checkoutSessionRepository
          .findByProviderAndProviderSessionId(PROVIDER, request.getSessionId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checkout session not found"));

      if (paymentEventRepository.existsByProviderAndEventId(PROVIDER, request.getEventId())) {
        return null;
      }

      Instant now = Instant.now(clock);
      paymentEventRepository.save(new PaymentEvent(PROVIDER, session.getTenantId(), request.getEventId(), now));

      if (session.getStatus() != CheckoutSessionStatus.COMPLETED) {
        session.markCompleted(now);
        checkoutSessionRepository.save(session);
      }

      return new CompletedCheckoutEvent(session.getTenantId(), session.getPlanCode(), session.getInterval());
    });
  }

  public record CompletedCheckoutEvent(String tenantId, String planCode, BillingInterval interval) {
  }
}

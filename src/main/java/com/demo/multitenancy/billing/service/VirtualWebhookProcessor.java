package com.demo.multitenancy.billing.service;

import com.demo.multitenancy.billing.webhook.virtual.VirtualWebhookRequest;
import com.demo.multitenancy.subscription.service.SubscriptionService;
import com.demo.multitenancy.tenant.TenantContextExecutor;
import com.demo.multitenancy.tenant.service.SignupCompletionService;

import org.springframework.stereotype.Service;

@Service
public class VirtualWebhookProcessor {

  private final VirtualWebhookControlPlaneService controlPlaneService;
  private final TenantContextExecutor tenantContextExecutor;
  private final SubscriptionService subscriptionService;
  private final SignupCompletionService signupCompletionService;

  public VirtualWebhookProcessor(
      VirtualWebhookControlPlaneService controlPlaneService,
      TenantContextExecutor tenantContextExecutor,
      SubscriptionService subscriptionService,
      SignupCompletionService signupCompletionService) {
    this.controlPlaneService = controlPlaneService;
    this.tenantContextExecutor = tenantContextExecutor;
    this.subscriptionService = subscriptionService;
    this.signupCompletionService = signupCompletionService;
  }

  public void handle(VirtualWebhookRequest request) {
    VirtualWebhookControlPlaneService.CompletedCheckoutEvent event = controlPlaneService.registerAndResolve(request);
    if (event == null) {
      // Duplicate/replayed webhook event; already processed.
      return;
    }

    // If this checkout session was created via /api/public/signup/checkout, complete tenant onboarding now.
    signupCompletionService.completeIfPending("virtual", event.sessionId());

    tenantContextExecutor.runWithTenant(event.tenantId(), () -> {
      subscriptionService.activatePaid(event.planCode(), event.interval());
      return null;
    });
  }
}

package com.demo.multitenancy.billing.service;

import com.demo.multitenancy.billing.webhook.virtual.VirtualWebhookRequest;
import com.demo.multitenancy.subscription.service.SubscriptionService;
import com.demo.multitenancy.tenant.TenantContextExecutor;

import org.springframework.stereotype.Service;

@Service
public class VirtualWebhookProcessor {

  private final VirtualWebhookControlPlaneService controlPlaneService;
  private final TenantContextExecutor tenantContextExecutor;
  private final SubscriptionService subscriptionService;

  public VirtualWebhookProcessor(
      VirtualWebhookControlPlaneService controlPlaneService,
      TenantContextExecutor tenantContextExecutor,
      SubscriptionService subscriptionService) {
    this.controlPlaneService = controlPlaneService;
    this.tenantContextExecutor = tenantContextExecutor;
    this.subscriptionService = subscriptionService;
  }

  public void handle(VirtualWebhookRequest request) {
    VirtualWebhookControlPlaneService.CompletedCheckoutEvent event = controlPlaneService.registerAndResolve(request);
    if (event == null) {
      // Duplicate/replayed webhook event; already processed.
      return;
    }

    tenantContextExecutor.runWithTenant(event.tenantId(), () -> {
      subscriptionService.activatePaid(event.planCode(), event.interval());
      return null;
    });
  }
}

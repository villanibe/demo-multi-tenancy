package com.demo.multitenancy.billing.api;

import java.util.Map;

import com.demo.multitenancy.billing.gateway.PaymentGateway;
import com.demo.multitenancy.billing.gateway.PaymentGatewayFactory;
import com.demo.multitenancy.user.service.TenantGuard;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.multitenancy.subscription.domain.BillingInterval;
import com.demo.multitenancy.subscription.domain.PlanPrice;
import com.demo.multitenancy.subscription.domain.PlanPriceRepository;
import com.demo.multitenancy.subscription.domain.PlanRepository;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
  private final PaymentGatewayFactory gatewayFactory;
  private final TenantGuard tenantGuard;
  private final PlanRepository planRepository;
  private final PlanPriceRepository planPriceRepository;

  public BillingController(
      PaymentGatewayFactory gatewayFactory,
      TenantGuard tenantGuard,
      PlanRepository planRepository,
      PlanPriceRepository planPriceRepository) {
    this.gatewayFactory = gatewayFactory;
    this.tenantGuard = tenantGuard;
    this.planRepository = planRepository;
    this.planPriceRepository = planPriceRepository;
  }

  @Operation(summary = "Create a checkout URL for a plan")
  @PreAuthorize("@tenantPermission.has('billing.write')")
  @PostMapping("/checkout")
  public ResponseEntity<Map<String, Object>> checkout(@Valid @RequestBody CheckoutRequest request) {
    String tenantId = tenantGuard.requireTenantId();
    PaymentGateway gateway = gatewayFactory.current();

    String planCode = request.getPlanCode();
    planRepository.findByCode(planCode)
        .orElseThrow(() -> new IllegalArgumentException("Unknown plan: " + planCode));

    PlanPrice planPrice = planPriceRepository
        .findByPlan_CodeAndInterval(planCode, request.getInterval())
        .orElseThrow(() -> new IllegalArgumentException("No price configured for plan " + planCode + " (" + request.getInterval() + ")"));

    String providerPriceId = planPrice.getProviderPriceIds().get(gateway.provider());
    if (providerPriceId == null || providerPriceId.isBlank()) {
      throw new IllegalArgumentException("No provider price id configured for provider '" + gateway.provider() + "' and plan " + planCode);
    }

    String url = gateway.createCheckoutUrl(tenantId, providerPriceId, planCode);
    return ResponseEntity.ok(Map.of(
        "provider", gateway.provider(),
        "url", url));
  }

  public static class CheckoutRequest {
    @NotBlank
    private String planCode;

    @NotNull
    private BillingInterval interval = BillingInterval.MONTHLY;

    public String getPlanCode() {
      return planCode;
    }

    public void setPlanCode(String planCode) {
      this.planCode = planCode;
    }

    public BillingInterval getInterval() {
      return interval;
    }

    public void setInterval(BillingInterval interval) {
      this.interval = interval;
    }
  }
}

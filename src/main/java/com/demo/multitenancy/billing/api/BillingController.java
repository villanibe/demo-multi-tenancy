package com.demo.multitenancy.billing.api;

import java.util.Map;

import com.demo.multitenancy.billing.gateway.PaymentGateway;
import com.demo.multitenancy.billing.gateway.PaymentGatewayFactory;
import com.demo.multitenancy.billing.service.BillingCheckoutService;
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
import org.springframework.web.bind.annotation.RequestHeader;

import com.demo.multitenancy.subscription.domain.BillingInterval;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
  private final PaymentGatewayFactory gatewayFactory;
  private final TenantGuard tenantGuard;
  private final BillingCheckoutService billingCheckoutService;

  public BillingController(
      PaymentGatewayFactory gatewayFactory,
      TenantGuard tenantGuard,
      BillingCheckoutService billingCheckoutService) {
    this.gatewayFactory = gatewayFactory;
    this.tenantGuard = tenantGuard;
    this.billingCheckoutService = billingCheckoutService;
  }

  @Operation(summary = "Create a checkout URL for a plan")
  @PreAuthorize("@tenantPermission.has('billing.write')")
  @PostMapping("/checkout")
  public ResponseEntity<Map<String, Object>> checkout(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody CheckoutRequest request) {
    String tenantId = tenantGuard.requireTenantId();
    PaymentGateway gateway = gatewayFactory.current();

    BillingCheckoutService.CheckoutSessionResult result = billingCheckoutService.createCheckout(
        tenantId,
        gateway,
        request.getPlanCode(),
        request.getInterval(),
        idempotencyKey);

    return ResponseEntity.ok(Map.of(
        "provider", gateway.provider(),
        "sessionId", result.sessionId(),
        "url", result.url()));
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

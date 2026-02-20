package com.demo.multitenancy.billing.api;

import java.util.Map;

import com.demo.multitenancy.billing.gateway.PaymentGateway;
import com.demo.multitenancy.billing.gateway.PaymentGatewayFactory;
import com.demo.multitenancy.user.service.TenantGuard;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
  private final PaymentGatewayFactory gatewayFactory;
  private final TenantGuard tenantGuard;

  public BillingController(PaymentGatewayFactory gatewayFactory, TenantGuard tenantGuard) {
    this.gatewayFactory = gatewayFactory;
    this.tenantGuard = tenantGuard;
  }

  @Operation(summary = "Create a checkout URL for a plan")
  @PreAuthorize("hasAuthority('SCOPE_admin') or hasRole('ADMIN')")
  @PostMapping("/checkout")
  public ResponseEntity<Map<String, Object>> checkout(@Valid @RequestBody CheckoutRequest request) {
    String tenantId = tenantGuard.requireTenantId();
    PaymentGateway gateway = gatewayFactory.current();
    String url = gateway.createCheckoutUrl(tenantId, request.getPlanCode());
    return ResponseEntity.ok(Map.of(
        "provider", gateway.provider(),
        "url", url));
  }

  public static class CheckoutRequest {
    @NotBlank
    private String planCode;

    public String getPlanCode() {
      return planCode;
    }

    public void setPlanCode(String planCode) {
      this.planCode = planCode;
    }
  }
}

package com.demo.multitenancy.tenant.api;

import java.util.Map;
import java.util.HashMap;

import com.demo.multitenancy.subscription.domain.BillingInterval;
import com.demo.multitenancy.tenant.service.PublicSignupService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/public/signup")
public class PublicSignupController {

  private final PublicSignupService publicSignupService;

  public PublicSignupController(PublicSignupService publicSignupService) {
    this.publicSignupService = publicSignupService;
  }

  @Operation(summary = "Start a public signup checkout (plan selection + payment before tenant onboarding)")
  @PostMapping("/checkout")
  public ResponseEntity<Map<String, Object>> checkout(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody StartSignupCheckoutRequest request) {
    try {
      PublicSignupService.SignupCheckoutResult result = publicSignupService.startCheckout(
          request.getTenantId(),
          request.getTenantDisplayName(),
          request.getOwnerSubject(),
          request.getOwnerEmail(),
          request.getOwnerPassword(),
          request.getPlanCode(),
          request.getInterval(),
          request.getMode(),
          idempotencyKey);

      Map<String, Object> response = new HashMap<>();
      response.put("mode", result.mode());
      response.put("provider", result.provider());
      response.put("tenantId", result.tenantId());
      response.put("sessionId", result.sessionId());
      response.put("url", result.url());
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException | IllegalStateException ex) {
      throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
    }
  }

  @Operation(summary = "Get signup session status by provider session id")
  @GetMapping("/sessions/{provider}/{sessionId}")
  public ResponseEntity<Map<String, Object>> sessionStatus(
      @PathVariable String provider,
      @PathVariable String sessionId) {
    return publicSignupService.getStatus(provider, sessionId)
        .map(status -> {
          Map<String, Object> response = new HashMap<>();
          response.put("provider", status.provider());
          response.put("sessionId", status.sessionId());
          response.put("tenantId", status.tenantId());
          response.put("planCode", status.planCode());
          response.put("interval", status.interval() != null ? status.interval().name() : null);
          response.put("checkoutStatus", status.checkoutStatus());
          response.put("signupStatus", status.signupStatus());
          response.put("tenantOnboarded", status.tenantOnboarded());
          return ResponseEntity.ok(response);
        })
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Signup session not found"));
  }

  public static class StartSignupCheckoutRequest {
    @NotBlank
    private String tenantId;

    @NotBlank
    private String tenantDisplayName;

    private String ownerSubject;

    @NotBlank
    private String ownerEmail;

    @NotBlank
    private String ownerPassword;

    @NotBlank
    private String planCode;

    @NotNull
    private BillingInterval interval = BillingInterval.MONTHLY;

    @NotNull
    private PublicSignupService.SignupMode mode = PublicSignupService.SignupMode.PAID;

    public String getTenantId() {
      return tenantId;
    }

    public void setTenantId(String tenantId) {
      this.tenantId = tenantId;
    }

    public String getTenantDisplayName() {
      return tenantDisplayName;
    }

    public void setTenantDisplayName(String tenantDisplayName) {
      this.tenantDisplayName = tenantDisplayName;
    }

    public String getOwnerSubject() {
      return ownerSubject;
    }

    public void setOwnerSubject(String ownerSubject) {
      this.ownerSubject = ownerSubject;
    }

    public String getOwnerEmail() {
      return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
      this.ownerEmail = ownerEmail;
    }

    public String getOwnerPassword() {
      return ownerPassword;
    }

    public void setOwnerPassword(String ownerPassword) {
      this.ownerPassword = ownerPassword;
    }

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

    public PublicSignupService.SignupMode getMode() {
      return mode;
    }

    public void setMode(PublicSignupService.SignupMode mode) {
      this.mode = mode;
    }
  }
}

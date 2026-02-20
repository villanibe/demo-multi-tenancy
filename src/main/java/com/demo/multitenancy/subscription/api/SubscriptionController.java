package com.demo.multitenancy.subscription.api;

import java.time.Instant;

import com.demo.multitenancy.subscription.domain.Subscription;
import com.demo.multitenancy.subscription.service.SubscriptionService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {
  private final SubscriptionService subscriptionService;

  public SubscriptionController(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @Operation(summary = "Get current tenant subscription")
  @PreAuthorize("@tenantPermission.has('subscription.read')")
  @GetMapping("/current")
  public ResponseEntity<SubscriptionResponse> current() {
    return ResponseEntity.ok(toResponse(subscriptionService.getCurrentSubscription()));
  }

  @Operation(summary = "Start trial if missing")
  @PreAuthorize("@tenantPermission.has('subscription.write')")
  @PostMapping("/trial")
  public ResponseEntity<SubscriptionResponse> startTrial(@RequestBody StartTrialRequest request) {
    Subscription subscription = subscriptionService.startTrialIfMissing(request.getPlanCode());
    return ResponseEntity.ok(toResponse(subscription));
  }

  private static SubscriptionResponse toResponse(Subscription subscription) {
    SubscriptionResponse response = new SubscriptionResponse();
    response.setPlanCode(subscription.getPlanCode());
    response.setStatus(subscription.getStatus().name());
    response.setTrialEndsAt(subscription.getTrialEndsAt());
    response.setCurrentPeriodEndsAt(subscription.getCurrentPeriodEndsAt());
    return response;
  }

  public static class StartTrialRequest {
    @NotBlank
    private String planCode;

    public String getPlanCode() {
      return planCode;
    }

    public void setPlanCode(String planCode) {
      this.planCode = planCode;
    }
  }

  public static class SubscriptionResponse {
    private String planCode;
    private String status;
    private Instant trialEndsAt;
    private Instant currentPeriodEndsAt;

    public String getPlanCode() {
      return planCode;
    }

    public void setPlanCode(String planCode) {
      this.planCode = planCode;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public Instant getTrialEndsAt() {
      return trialEndsAt;
    }

    public void setTrialEndsAt(Instant trialEndsAt) {
      this.trialEndsAt = trialEndsAt;
    }

    public Instant getCurrentPeriodEndsAt() {
      return currentPeriodEndsAt;
    }

    public void setCurrentPeriodEndsAt(Instant currentPeriodEndsAt) {
      this.currentPeriodEndsAt = currentPeriodEndsAt;
    }
  }
}

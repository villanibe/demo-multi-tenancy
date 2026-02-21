package com.demo.multitenancy.tenant.service;

import java.time.Clock;
import java.time.Instant;

import com.demo.multitenancy.tenant.domain.PendingTenantSignup;
import com.demo.multitenancy.tenant.domain.PendingTenantSignupRepository;
import com.demo.multitenancy.tenant.domain.PendingTenantSignupStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupCompletionService {

  private final PendingTenantSignupRepository pendingTenantSignupRepository;
  private final TenantOnboardingService tenantOnboardingService;
  private final ControlPlaneExecutor controlPlane;
  private final Clock clock;

  public SignupCompletionService(
      PendingTenantSignupRepository pendingTenantSignupRepository,
      TenantOnboardingService tenantOnboardingService,
      ControlPlaneExecutor controlPlane,
      Clock clock) {
    this.pendingTenantSignupRepository = pendingTenantSignupRepository;
    this.tenantOnboardingService = tenantOnboardingService;
    this.controlPlane = controlPlane;
    this.clock = clock;
  }

  @Transactional
  public void completeIfPending(String provider, String providerSessionId) {
    controlPlane.run(() -> {
      PendingTenantSignup pending = pendingTenantSignupRepository
          .findByProviderAndProviderSessionId(provider, providerSessionId)
          .orElse(null);

      if (pending == null) {
        return null;
      }
      if (pending.getStatus() == PendingTenantSignupStatus.COMPLETED) {
        return null;
      }

      tenantOnboardingService.ensureTenantExistsAsOwner(
          pending.getTenantId(),
          pending.getTenantDisplayName(),
          pending.getOwnerSubject(),
          pending.getOwnerEmail());

      Instant now = Instant.now(clock);
      pending.markCompleted(now);
      pendingTenantSignupRepository.save(pending);
      return null;
    });
  }
}

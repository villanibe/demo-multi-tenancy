package com.demo.multitenancy.tenant.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import com.demo.multitenancy.billing.domain.CheckoutSession;
import com.demo.multitenancy.billing.domain.CheckoutSessionRepository;
import com.demo.multitenancy.billing.gateway.PaymentGateway;
import com.demo.multitenancy.billing.gateway.PaymentGatewayFactory;
import com.demo.multitenancy.billing.service.BillingCheckoutService;
import com.demo.multitenancy.subscription.domain.BillingInterval;
import com.demo.multitenancy.subscription.service.SubscriptionService;
import com.demo.multitenancy.security.auth.domain.Identity;
import com.demo.multitenancy.security.auth.service.IdentityService;
import com.demo.multitenancy.tenant.TenantContextExecutor;
import com.demo.multitenancy.tenant.domain.PendingTenantSignup;
import com.demo.multitenancy.tenant.domain.PendingTenantSignupRepository;
import com.demo.multitenancy.tenant.domain.TenantRegistrationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicSignupService {

  private final PaymentGatewayFactory gatewayFactory;
  private final BillingCheckoutService billingCheckoutService;
  private final CheckoutSessionRepository checkoutSessionRepository;
  private final PendingTenantSignupRepository pendingTenantSignupRepository;
  private final TenantRegistrationRepository tenantRegistrationRepository;
  private final TenantOnboardingService tenantOnboardingService;
  private final SubscriptionService subscriptionService;
  private final TenantContextExecutor tenantContextExecutor;
  private final IdentityService identityService;
  private final ControlPlaneExecutor controlPlane;
  private final Clock clock;

  public PublicSignupService(
      PaymentGatewayFactory gatewayFactory,
      BillingCheckoutService billingCheckoutService,
      CheckoutSessionRepository checkoutSessionRepository,
      PendingTenantSignupRepository pendingTenantSignupRepository,
      TenantRegistrationRepository tenantRegistrationRepository,
      TenantOnboardingService tenantOnboardingService,
      SubscriptionService subscriptionService,
      TenantContextExecutor tenantContextExecutor,
      IdentityService identityService,
      ControlPlaneExecutor controlPlane,
      Clock clock) {
    this.gatewayFactory = gatewayFactory;
    this.billingCheckoutService = billingCheckoutService;
    this.checkoutSessionRepository = checkoutSessionRepository;
    this.pendingTenantSignupRepository = pendingTenantSignupRepository;
    this.tenantRegistrationRepository = tenantRegistrationRepository;
    this.tenantOnboardingService = tenantOnboardingService;
    this.subscriptionService = subscriptionService;
    this.tenantContextExecutor = tenantContextExecutor;
    this.identityService = identityService;
    this.controlPlane = controlPlane;
    this.clock = clock;
  }

  @Transactional
  public SignupCheckoutResult startCheckout(
      String tenantId,
      String tenantDisplayName,
      String ownerSubject,
      String ownerEmail,
      String ownerPassword,
      String planCode,
      BillingInterval interval,
      SignupMode mode,
      String idempotencyKey) {

    if (mode == null) {
      mode = SignupMode.PAID;
    }

    Instant now = Instant.now(clock);

    controlPlane.run(() -> {
      tenantRegistrationRepository.findByTenantId(tenantId).ifPresent(existing -> {
        throw new IllegalArgumentException("Tenant already exists: " + tenantId);
      });
      return null;
    });

    Identity identity = identityService.ensureIdentityExists(ownerEmail, ownerPassword, ownerSubject);

    if (mode == SignupMode.TRIAL) {
      tenantOnboardingService.ensureTenantExistsAsOwner(tenantId, tenantDisplayName, identity.getSubject(), identity.getEmail());
      tenantContextExecutor.runWithTenant(tenantId, () -> subscriptionService.startTrialIfMissing(planCode));

      controlPlane.run(() -> {
        pendingTenantSignupRepository.findByProviderAndProviderSessionId("trial", tenantId)
            .orElseGet(() -> {
              PendingTenantSignup pending = new PendingTenantSignup(
                  "trial",
                  tenantId,
                  tenantId,
                  tenantDisplayName,
                  identity.getSubject(),
                  identity.getEmail(),
                  now);
              pending.markCompleted(now);
              return pendingTenantSignupRepository.save(pending);
            });
        return null;
      });

      return new SignupCheckoutResult("trial", tenantId, tenantId, null, SignupMode.TRIAL.name());
    }

    PaymentGateway gateway = gatewayFactory.current();

    BillingCheckoutService.CheckoutSessionResult checkout = billingCheckoutService.createCheckout(
        tenantId,
        gateway,
        planCode,
        interval,
        idempotencyKey);

    controlPlane.run(() -> {
      pendingTenantSignupRepository.findByProviderAndProviderSessionId(gateway.provider(), checkout.sessionId())
          .orElseGet(() -> pendingTenantSignupRepository.save(new PendingTenantSignup(
              gateway.provider(),
              checkout.sessionId(),
              tenantId,
              tenantDisplayName,
              identity.getSubject(),
              identity.getEmail(),
              now)));
      return null;
    });

    return new SignupCheckoutResult(gateway.provider(), tenantId, checkout.sessionId(), checkout.url(), SignupMode.PAID.name());
  }

  @Transactional(readOnly = true)
  public Optional<SignupSessionStatus> getStatus(String provider, String sessionId) {
    return controlPlane.run(() -> {
      Optional<CheckoutSession> session = checkoutSessionRepository.findByProviderAndProviderSessionId(provider, sessionId);
      Optional<PendingTenantSignup> pending = pendingTenantSignupRepository.findByProviderAndProviderSessionId(provider, sessionId);
      boolean tenantExists = pending.map(p -> tenantRegistrationRepository.findByTenantId(p.getTenantId()).isPresent()).orElse(false);

      if (session.isEmpty() && pending.isEmpty()) {
        return Optional.empty();
      }

      String tenantId = pending.map(PendingTenantSignup::getTenantId).orElseGet(() -> session.map(CheckoutSession::getTenantId).orElse(null));
      String planCode = session.map(CheckoutSession::getPlanCode).orElse(null);
      BillingInterval interval = session.map(CheckoutSession::getInterval).orElse(null);
      String checkoutStatus = session.map(s -> s.getStatus().name()).orElse(null);
      String signupStatus = pending.map(p -> p.getStatus().name()).orElse(null);

      return Optional.of(new SignupSessionStatus(
          provider,
          sessionId,
          tenantId,
          planCode,
          interval,
          checkoutStatus,
          signupStatus,
          tenantExists));
    });
  }

  public enum SignupMode {
    PAID,
    TRIAL
  }

  public record SignupCheckoutResult(String provider, String tenantId, String sessionId, String url, String mode) {
  }

  public record SignupSessionStatus(
      String provider,
      String sessionId,
      String tenantId,
      String planCode,
      BillingInterval interval,
      String checkoutStatus,
      String signupStatus,
      boolean tenantOnboarded) {
  }
}

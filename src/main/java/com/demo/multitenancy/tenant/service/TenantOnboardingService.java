package com.demo.multitenancy.tenant.service;

import java.time.Clock;
import java.time.Instant;

import com.demo.multitenancy.security.principal.CurrentPrincipal;
import com.demo.multitenancy.security.principal.PrincipalInfo;
import com.demo.multitenancy.tenant.domain.TenantMembership;
import com.demo.multitenancy.tenant.domain.TenantMembershipRepository;
import com.demo.multitenancy.tenant.domain.TenantRegistration;
import com.demo.multitenancy.tenant.domain.TenantRegistrationRepository;
import com.demo.multitenancy.tenant.domain.TenantRole;
import com.demo.multitenancy.user.service.TenantAuthorizationBootstrapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantOnboardingService {
  private final TenantRegistrationRepository tenantRegistrationRepository;
  private final TenantMembershipRepository tenantMembershipRepository;
  private final CurrentPrincipal currentPrincipal;
  private final ControlPlaneExecutor controlPlane;
  private final TenantAuthorizationBootstrapper authorizationBootstrapper;
  private final Clock clock;

  public TenantOnboardingService(
      TenantRegistrationRepository tenantRegistrationRepository,
      TenantMembershipRepository tenantMembershipRepository,
      CurrentPrincipal currentPrincipal,
      ControlPlaneExecutor controlPlane,
      TenantAuthorizationBootstrapper authorizationBootstrapper,
      Clock clock) {
    this.tenantRegistrationRepository = tenantRegistrationRepository;
    this.tenantMembershipRepository = tenantMembershipRepository;
    this.currentPrincipal = currentPrincipal;
    this.controlPlane = controlPlane;
    this.authorizationBootstrapper = authorizationBootstrapper;
    this.clock = clock;
  }

  @Transactional
  public TenantRegistration createTenant(String tenantId, String displayName) {
    PrincipalInfo principal = currentPrincipal.require();

    TenantRegistration created = controlPlane.run(() -> {
      tenantRegistrationRepository.findByTenantId(tenantId).ifPresent(existing -> {
        throw new IllegalArgumentException("Tenant already exists: " + tenantId);
      });

      Instant now = Instant.now(clock);
      TenantRegistration registration = tenantRegistrationRepository.save(new TenantRegistration(tenantId, displayName, now));
      tenantMembershipRepository.save(new TenantMembership(tenantId, principal.getSubject(), principal.getEmail(), TenantRole.OWNER, now));
      return registration;
    });

    authorizationBootstrapper.bootstrapIfMissing(tenantId);
    return created;
  }

  @Transactional
  public TenantRegistration ensureTenantExistsAsOwner(String tenantId, String displayName, String ownerSubject, String ownerEmail) {
    return controlPlane.run(() -> {
      return tenantRegistrationRepository.findByTenantId(tenantId)
          .orElseGet(() -> createTenantAsOwner(tenantId, displayName, ownerSubject, ownerEmail));
    });
  }

  @Transactional
  public TenantRegistration createTenantAsOwner(String tenantId, String displayName, String ownerSubject, String ownerEmail) {
    if (tenantId == null || tenantId.isBlank()) {
      throw new IllegalArgumentException("tenantId is required");
    }
    if (displayName == null || displayName.isBlank()) {
      throw new IllegalArgumentException("displayName is required");
    }
    if (ownerSubject == null || ownerSubject.isBlank()) {
      throw new IllegalArgumentException("ownerSubject is required");
    }

    TenantRegistration created = controlPlane.run(() -> {
      tenantRegistrationRepository.findByTenantId(tenantId).ifPresent(existing -> {
        throw new IllegalArgumentException("Tenant already exists: " + tenantId);
      });

      Instant now = Instant.now(clock);
      TenantRegistration registration = tenantRegistrationRepository.save(new TenantRegistration(tenantId, displayName, now));
      tenantMembershipRepository.save(new TenantMembership(tenantId, ownerSubject, ownerEmail, TenantRole.OWNER, now));
      return registration;
    });

    authorizationBootstrapper.bootstrapIfMissing(tenantId);
    return created;
  }
}

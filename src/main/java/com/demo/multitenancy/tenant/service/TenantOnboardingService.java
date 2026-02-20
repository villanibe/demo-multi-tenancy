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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantOnboardingService {
  private final TenantRegistrationRepository tenantRegistrationRepository;
  private final TenantMembershipRepository tenantMembershipRepository;
  private final CurrentPrincipal currentPrincipal;
  private final ControlPlaneExecutor controlPlane;
  private final Clock clock;

  public TenantOnboardingService(
      TenantRegistrationRepository tenantRegistrationRepository,
      TenantMembershipRepository tenantMembershipRepository,
      CurrentPrincipal currentPrincipal,
      ControlPlaneExecutor controlPlane,
      Clock clock) {
    this.tenantRegistrationRepository = tenantRegistrationRepository;
    this.tenantMembershipRepository = tenantMembershipRepository;
    this.currentPrincipal = currentPrincipal;
    this.controlPlane = controlPlane;
    this.clock = clock;
  }

  @Transactional
  public TenantRegistration createTenant(String tenantId, String displayName) {
    PrincipalInfo principal = currentPrincipal.require();

    return controlPlane.run(() -> {
      tenantRegistrationRepository.findByTenantId(tenantId).ifPresent(existing -> {
        throw new IllegalArgumentException("Tenant already exists: " + tenantId);
      });

      Instant now = Instant.now(clock);
      TenantRegistration created = tenantRegistrationRepository.save(new TenantRegistration(tenantId, displayName, now));
      tenantMembershipRepository.save(new TenantMembership(tenantId, principal.getSubject(), principal.getEmail(), TenantRole.OWNER, now));
      return created;
    });
  }
}

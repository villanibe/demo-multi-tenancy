package com.demo.multitenancy.tenant.service;

import java.util.List;

import com.demo.multitenancy.security.principal.CurrentPrincipal;
import com.demo.multitenancy.security.principal.PrincipalInfo;
import com.demo.multitenancy.tenant.domain.TenantMembership;
import com.demo.multitenancy.tenant.domain.TenantMembershipRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantMembershipService {
  private final TenantMembershipRepository repository;
  private final CurrentPrincipal currentPrincipal;
  private final ControlPlaneExecutor controlPlane;

  public TenantMembershipService(TenantMembershipRepository repository, CurrentPrincipal currentPrincipal, ControlPlaneExecutor controlPlane) {
    this.repository = repository;
    this.currentPrincipal = currentPrincipal;
    this.controlPlane = controlPlane;
  }

  @Transactional(readOnly = true)
  public List<TenantMembership> myMemberships() {
    PrincipalInfo principal = currentPrincipal.require();
    return controlPlane.run(() -> repository.findAllBySubjectOrderByCreatedAtAsc(principal.getSubject()));
  }

  @Transactional(readOnly = true)
  public TenantMembership requireMembership(String tenantId) {
    PrincipalInfo principal = currentPrincipal.require();
    return controlPlane.run(() -> repository.findByTenantIdAndSubject(tenantId, principal.getSubject())
        .orElseThrow(() -> new IllegalStateException("Not a member of tenant: " + tenantId)));
  }
}

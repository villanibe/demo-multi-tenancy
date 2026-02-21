package com.demo.multitenancy.tenant.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.demo.multitenancy.security.principal.CurrentPrincipal;
import com.demo.multitenancy.security.principal.PrincipalInfo;
import com.demo.multitenancy.tenant.domain.TenantInvite;
import com.demo.multitenancy.tenant.domain.TenantInviteRepository;
import com.demo.multitenancy.tenant.domain.TenantInviteStatus;
import com.demo.multitenancy.tenant.domain.TenantMembership;
import com.demo.multitenancy.tenant.domain.TenantMembershipRepository;
import com.demo.multitenancy.tenant.domain.TenantRole;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantInviteService {
  private static final long DEFAULT_INVITE_TTL_DAYS = 7;

  private final TenantInviteRepository inviteRepository;
  private final TenantMembershipRepository membershipRepository;
  private final CurrentPrincipal currentPrincipal;
  private final ControlPlaneExecutor controlPlane;
  private final Clock clock;

  public TenantInviteService(
      TenantInviteRepository inviteRepository,
      TenantMembershipRepository membershipRepository,
      CurrentPrincipal currentPrincipal,
      ControlPlaneExecutor controlPlane,
      Clock clock) {
    this.inviteRepository = inviteRepository;
    this.membershipRepository = membershipRepository;
    this.currentPrincipal = currentPrincipal;
    this.controlPlane = controlPlane;
    this.clock = clock;
  }

  @Transactional
  public TenantInvite createInvite(String tenantId, String email, TenantRole role) {
    PrincipalInfo principal = currentPrincipal.require();

    return controlPlane.run(() -> {
      TenantMembership inviter = membershipRepository.findByTenantIdAndSubject(tenantId, principal.getSubject())
          .orElseThrow(() -> new IllegalStateException("Not a member of tenant: " + tenantId));

      if (inviter.getRole() != TenantRole.OWNER && inviter.getRole() != TenantRole.ADMIN) {
        throw new IllegalStateException("Not allowed to invite members for tenant: " + tenantId);
      }

      Instant now = Instant.now(clock);
      String token = UUID.randomUUID().toString();
      Instant expiresAt = now.plus(DEFAULT_INVITE_TTL_DAYS, ChronoUnit.DAYS);
      return inviteRepository.save(new TenantInvite(tenantId, email, token, role != null ? role : TenantRole.MEMBER, now, expiresAt));
    });
  }

  @Transactional
  public TenantMembership acceptInvite(String token) {
    PrincipalInfo principal = currentPrincipal.require();

    return acceptInviteAs(token, principal.getSubject(), principal.getEmail());
  }

  @Transactional
  public TenantMembership acceptInviteAs(String token, String subject, String email) {
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("subject is required");
    }
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("email is required");
    }

    return controlPlane.run(() -> {
      TenantInvite invite = inviteRepository.findByToken(token)
          .orElseThrow(() -> new IllegalArgumentException("Invite not found"));

      Instant now = Instant.now(clock);
      if (invite.getStatus() != TenantInviteStatus.PENDING) {
        throw new IllegalStateException("Invite not pending");
      }
      if (invite.isExpired(now)) {
        invite.expire();
        throw new IllegalStateException("Invite expired");
      }

      if (invite.getEmail() != null && !invite.getEmail().equalsIgnoreCase(email)) {
        throw new IllegalStateException("Invite email mismatch");
      }

      TenantMembership membership = membershipRepository.findByTenantIdAndSubject(invite.getTenantId(), subject)
          .orElseGet(() -> membershipRepository.save(
              new TenantMembership(invite.getTenantId(), subject, email, invite.getRole() != null ? invite.getRole() : TenantRole.MEMBER, now)));

      invite.accept(now);
      return membership;
    });
  }
}

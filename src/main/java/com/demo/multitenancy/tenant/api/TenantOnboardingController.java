package com.demo.multitenancy.tenant.api;

import java.time.Instant;
import java.util.List;

import com.demo.multitenancy.tenant.domain.TenantInvite;
import com.demo.multitenancy.tenant.domain.TenantMembership;
import com.demo.multitenancy.tenant.domain.TenantRegistration;
import com.demo.multitenancy.tenant.service.TenantInviteService;
import com.demo.multitenancy.tenant.service.TenantMembershipService;
import com.demo.multitenancy.tenant.service.TenantOnboardingService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/tenants")
public class TenantOnboardingController {
  private final TenantOnboardingService onboardingService;
  private final TenantMembershipService membershipService;
  private final TenantInviteService inviteService;

  public TenantOnboardingController(
      TenantOnboardingService onboardingService,
      TenantMembershipService membershipService,
      TenantInviteService inviteService) {
    this.onboardingService = onboardingService;
    this.membershipService = membershipService;
    this.inviteService = inviteService;
  }

  @Operation(summary = "Create a tenant and grant OWNER membership to the creator")
  @PreAuthorize("isAuthenticated()")
  @PostMapping
  public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
    TenantRegistration created = onboardingService.createTenant(request.getTenantId(), request.getDisplayName());
    return ResponseEntity.ok(toResponse(created));
  }

  @Operation(summary = "List my tenant memberships")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/mine")
  public ResponseEntity<List<TenantMembershipResponse>> mine() {
    return ResponseEntity.ok(membershipService.myMemberships().stream().map(TenantOnboardingController::toResponse).toList());
  }

  @Operation(summary = "Invite a member to a tenant")
  @PreAuthorize("@tenantPermission.hasForTenant(#tenantId, 'tenant.invite.write')")
  @PostMapping("/{tenantId}/invites")
  public ResponseEntity<TenantInviteResponse> invite(
      @PathVariable String tenantId,
      @Valid @RequestBody CreateInviteRequest request) {
    try {
      TenantInvite invite = inviteService.createInvite(tenantId, request.getEmail());
      return ResponseEntity.ok(toResponse(invite));
    } catch (IllegalArgumentException | IllegalStateException ex) {
      throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
    }
  }

  @Operation(summary = "Accept an invite by token")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/invites/accept")
  public ResponseEntity<TenantMembershipResponse> accept(@Valid @RequestBody AcceptInviteRequest request) {
    try {
      TenantMembership membership = inviteService.acceptInvite(request.getToken());
      return ResponseEntity.ok(toResponse(membership));
    } catch (IllegalArgumentException | IllegalStateException ex) {
      throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
    }
  }

  private static TenantResponse toResponse(TenantRegistration tenant) {
    TenantResponse response = new TenantResponse();
    response.setTenantId(tenant.getTenantId());
    response.setDisplayName(tenant.getDisplayName());
    response.setCreatedAt(tenant.getCreatedAt());
    return response;
  }

  private static TenantMembershipResponse toResponse(TenantMembership membership) {
    TenantMembershipResponse response = new TenantMembershipResponse();
    response.setTenantId(membership.getTenantId());
    response.setSubject(membership.getSubject());
    response.setEmail(membership.getEmail());
    response.setRole(membership.getRole().name());
    response.setCreatedAt(membership.getCreatedAt());
    return response;
  }

  private static TenantInviteResponse toResponse(TenantInvite invite) {
    TenantInviteResponse response = new TenantInviteResponse();
    response.setTenantId(invite.getTenantId());
    response.setEmail(invite.getEmail());
    response.setToken(invite.getToken());
    response.setStatus(invite.getStatus().name());
    response.setExpiresAt(invite.getExpiresAt());
    return response;
  }

  public static class CreateTenantRequest {
    @NotBlank
    private String tenantId;

    @NotBlank
    private String displayName;

    public String getTenantId() {
      return tenantId;
    }

    public void setTenantId(String tenantId) {
      this.tenantId = tenantId;
    }

    public String getDisplayName() {
      return displayName;
    }

    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }
  }

  public static class TenantResponse {
    private String tenantId;
    private String displayName;
    private Instant createdAt;

    public String getTenantId() {
      return tenantId;
    }

    public void setTenantId(String tenantId) {
      this.tenantId = tenantId;
    }

    public String getDisplayName() {
      return displayName;
    }

    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }

    public Instant getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
      this.createdAt = createdAt;
    }
  }

  public static class CreateInviteRequest {
    @NotBlank
    private String email;

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }
  }

  public static class AcceptInviteRequest {
    @NotBlank
    private String token;

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }
  }

  public static class TenantMembershipResponse {
    private String tenantId;
    private String subject;
    private String email;
    private String role;
    private Instant createdAt;

    public String getTenantId() {
      return tenantId;
    }

    public void setTenantId(String tenantId) {
      this.tenantId = tenantId;
    }

    public String getSubject() {
      return subject;
    }

    public void setSubject(String subject) {
      this.subject = subject;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getRole() {
      return role;
    }

    public void setRole(String role) {
      this.role = role;
    }

    public Instant getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
      this.createdAt = createdAt;
    }
  }

  public static class TenantInviteResponse {
    private String tenantId;
    private String email;
    private String token;
    private String status;
    private Instant expiresAt;

    public String getTenantId() {
      return tenantId;
    }

    public void setTenantId(String tenantId) {
      this.tenantId = tenantId;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public Instant getExpiresAt() {
      return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
      this.expiresAt = expiresAt;
    }
  }
}

package com.demo.multitenancy.tenant.api;

import java.time.Instant;

import com.demo.multitenancy.security.auth.domain.Identity;
import com.demo.multitenancy.security.auth.service.IdentityService;
import com.demo.multitenancy.security.auth.service.LocalAuthService;
import com.demo.multitenancy.tenant.domain.TenantMembership;
import com.demo.multitenancy.tenant.service.TenantInviteService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/public/invites")
@ConditionalOnProperty(prefix = "app.security.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PublicInviteController {

  private final IdentityService identityService;
  private final LocalAuthService authService;
  private final TenantInviteService tenantInviteService;

  public PublicInviteController(IdentityService identityService, LocalAuthService authService, TenantInviteService tenantInviteService) {
    this.identityService = identityService;
    this.authService = authService;
    this.tenantInviteService = tenantInviteService;
  }

  @Operation(summary = "Accept an invite (public): create user if needed, create membership, and return tokens")
  @PostMapping("/accept")
  public ResponseEntity<AcceptInviteResponse> accept(@Valid @RequestBody AcceptInviteRequest request) {
    try {
      Identity identity = identityService.ensureIdentityForInviteAccept(request.getEmail(), request.getPassword(), null);
      TenantMembership membership = tenantInviteService.acceptInviteAs(request.getToken(), identity.getSubject(), identity.getEmail());
      LocalAuthService.TokenResponse tokens = authService.issueTokens(identity);

      AcceptInviteResponse response = new AcceptInviteResponse();
      response.setTenantId(membership.getTenantId());
      response.setSubject(membership.getSubject());
      response.setEmail(membership.getEmail());
      response.setRole(membership.getRole().name());
      response.setCreatedAt(membership.getCreatedAt());
      response.setAccessToken(tokens.accessToken());
      response.setAccessTokenExpiresInSeconds(tokens.accessTokenExpiresInSeconds());
      response.setRefreshToken(tokens.refreshToken());
      response.setRefreshTokenExpiresInSeconds(tokens.refreshTokenExpiresInSeconds());

      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException | IllegalStateException ex) {
      throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
    }
  }

  public static class AcceptInviteRequest {
    @NotBlank
    private String token;

    @NotBlank
    private String email;

    @NotBlank
    private String password;

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }

  public static class AcceptInviteResponse {
    private String tenantId;
    private String subject;
    private String email;
    private String role;
    private Instant createdAt;

    private String accessToken;
    private long accessTokenExpiresInSeconds;
    private String refreshToken;
    private long refreshTokenExpiresInSeconds;

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

    public String getAccessToken() {
      return accessToken;
    }

    public void setAccessToken(String accessToken) {
      this.accessToken = accessToken;
    }

    public long getAccessTokenExpiresInSeconds() {
      return accessTokenExpiresInSeconds;
    }

    public void setAccessTokenExpiresInSeconds(long accessTokenExpiresInSeconds) {
      this.accessTokenExpiresInSeconds = accessTokenExpiresInSeconds;
    }

    public String getRefreshToken() {
      return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
      this.refreshToken = refreshToken;
    }

    public long getRefreshTokenExpiresInSeconds() {
      return refreshTokenExpiresInSeconds;
    }

    public void setRefreshTokenExpiresInSeconds(long refreshTokenExpiresInSeconds) {
      this.refreshTokenExpiresInSeconds = refreshTokenExpiresInSeconds;
    }
  }
}

package com.demo.multitenancy.security.auth.api;

import com.demo.multitenancy.security.auth.service.LocalAuthService;

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
@RequestMapping("/api/public/auth")
@ConditionalOnProperty(prefix = "app.security.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PublicAuthController {

  private final LocalAuthService authService;

  public PublicAuthController(LocalAuthService authService) {
    this.authService = authService;
  }

  @Operation(summary = "Login with email + password and receive access/refresh tokens")
  @PostMapping("/login")
  public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    try {
      LocalAuthService.TokenResponse tokens = authService.login(request.getEmail(), request.getPassword());
      return ResponseEntity.ok(TokenResponse.from(tokens));
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
    }
  }

  @Operation(summary = "Rotate refresh token and receive a new access token")
  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    try {
      LocalAuthService.TokenResponse tokens = authService.refresh(request.getRefreshToken());
      return ResponseEntity.ok(TokenResponse.from(tokens));
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
    }
  }

  @Operation(summary = "Revoke a refresh token")
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
    authService.logout(request.getRefreshToken());
    return ResponseEntity.noContent().build();
  }

  public static class LoginRequest {
    @NotBlank
    private String email;

    @NotBlank
    private String password;

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

  public static class RefreshRequest {
    @NotBlank
    private String refreshToken;

    public String getRefreshToken() {
      return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
      this.refreshToken = refreshToken;
    }
  }

  public static class TokenResponse {
    private String accessToken;
    private long accessTokenExpiresInSeconds;
    private String refreshToken;
    private long refreshTokenExpiresInSeconds;

    public static TokenResponse from(LocalAuthService.TokenResponse tokens) {
      TokenResponse r = new TokenResponse();
      r.accessToken = tokens.accessToken();
      r.accessTokenExpiresInSeconds = tokens.accessTokenExpiresInSeconds();
      r.refreshToken = tokens.refreshToken();
      r.refreshTokenExpiresInSeconds = tokens.refreshTokenExpiresInSeconds();
      return r;
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

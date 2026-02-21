package com.demo.multitenancy.security.auth.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.demo.multitenancy.security.auth.AuthProperties;
import com.demo.multitenancy.security.auth.domain.IdentityRepository;
import com.demo.multitenancy.security.auth.domain.Identity;
import com.demo.multitenancy.security.auth.domain.RefreshToken;
import com.demo.multitenancy.security.auth.domain.RefreshTokenRepository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "app.security.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LocalAuthService {

  private final IdentityService identityService;
  private final IdentityRepository identityRepository;
  private final JwtTokenService jwtTokenService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final AuthProperties authProperties;
  private final Clock clock;

  private final SecureRandom secureRandom = new SecureRandom();

  public LocalAuthService(
      IdentityService identityService,
      IdentityRepository identityRepository,
      JwtTokenService jwtTokenService,
      RefreshTokenRepository refreshTokenRepository,
      AuthProperties authProperties,
      Clock clock) {
    this.identityService = identityService;
    this.identityRepository = identityRepository;
    this.jwtTokenService = jwtTokenService;
    this.refreshTokenRepository = refreshTokenRepository;
    this.authProperties = authProperties;
    this.clock = clock;
  }

  @Transactional
  public TokenResponse login(String email, String password) {
    Identity identity = identityService.ensureIdentityForLogin(email, password);
    return issueTokens(identity);
  }

  @Transactional
  public TokenResponse issueTokens(Identity identity) {
    JwtTokenService.IssuedAccessToken access = jwtTokenService.issueAccessToken(identity.getSubject(), identity.getEmail());
    IssuedRefreshToken refresh = issueRefreshToken(identity.getSubject());
    return new TokenResponse(access.token(), access.expiresInSeconds(), refresh.refreshToken(), refresh.expiresInSeconds());
  }

  @Transactional
  public TokenResponse refresh(String rawRefreshToken) {
    String hash = TokenHashing.sha256Hex(rawRefreshToken);

    RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
        .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

    Instant now = Instant.now(clock);
    if (!existing.isActive(now)) {
      throw new IllegalArgumentException("Refresh token not active");
    }

    String email = identityRepository.findBySubject(existing.getSubject())
        .map(Identity::getEmail)
        .orElse(null);
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Identity not found");
    }

    JwtTokenService.IssuedAccessToken access = jwtTokenService.issueAccessToken(existing.getSubject(), email);
    IssuedRefreshToken rotated = issueRefreshToken(existing.getSubject());

    existing.revoke(now, rotated.tokenId());

    return new TokenResponse(access.token(), access.expiresInSeconds(), rotated.refreshToken(), rotated.expiresInSeconds());
  }

  @Transactional
  public void logout(String rawRefreshToken) {
    String hash = TokenHashing.sha256Hex(rawRefreshToken);
    refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> rt.revoke(Instant.now(clock), null));
  }

  private IssuedRefreshToken issueRefreshToken(String subject) {
    byte[] raw = new byte[48];
    secureRandom.nextBytes(raw);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

    String hash = TokenHashing.sha256Hex(token);

    Instant now = Instant.now(clock);
    Instant exp = now.plusSeconds(authProperties.getRefreshTokenTtlSeconds());

    RefreshToken saved = refreshTokenRepository.save(new RefreshToken(subject, hash, now, exp));

    return new IssuedRefreshToken(saved.getId(), token, authProperties.getRefreshTokenTtlSeconds());
  }

  public record TokenResponse(String accessToken, long accessTokenExpiresInSeconds, String refreshToken,
      long refreshTokenExpiresInSeconds) {
  }

  private record IssuedRefreshToken(UUID tokenId, String refreshToken, long expiresInSeconds) {
  }
}

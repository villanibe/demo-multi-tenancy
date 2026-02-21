package com.demo.multitenancy.security.auth.service;

import java.time.Clock;
import java.time.Instant;

import com.demo.multitenancy.security.auth.AuthProperties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.security.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JwtTokenService {

  private final JwtEncoder jwtEncoder;
  private final AuthProperties authProperties;
  private final Clock clock;

  public JwtTokenService(JwtEncoder jwtEncoder, AuthProperties authProperties, Clock clock) {
    this.jwtEncoder = jwtEncoder;
    this.authProperties = authProperties;
    this.clock = clock;
  }

  public IssuedAccessToken issueAccessToken(String subject, String email) {
    Instant now = Instant.now(clock);
    Instant exp = now.plusSeconds(authProperties.getAccessTokenTtlSeconds());

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuedAt(now)
        .expiresAt(exp)
        .subject(subject)
        .claim("email", email)
        .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

    return new IssuedAccessToken(tokenValue, authProperties.getAccessTokenTtlSeconds());
  }

  public record IssuedAccessToken(String token, long expiresInSeconds) {
  }
}

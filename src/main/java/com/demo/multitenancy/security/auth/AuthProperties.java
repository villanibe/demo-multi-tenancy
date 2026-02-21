package com.demo.multitenancy.security.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.auth")
public class AuthProperties {
  private boolean enabled = true;

  /** Access token TTL in seconds. */
  private long accessTokenTtlSeconds = 900;

  /** Refresh token TTL in seconds. */
  private long refreshTokenTtlSeconds = 1209600;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public long getAccessTokenTtlSeconds() {
    return accessTokenTtlSeconds;
  }

  public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) {
    this.accessTokenTtlSeconds = accessTokenTtlSeconds;
  }

  public long getRefreshTokenTtlSeconds() {
    return refreshTokenTtlSeconds;
  }

  public void setRefreshTokenTtlSeconds(long refreshTokenTtlSeconds) {
    this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
  }
}

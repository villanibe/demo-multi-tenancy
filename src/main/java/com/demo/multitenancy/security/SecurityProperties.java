package com.demo.multitenancy.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
  private final Jwt jwt = new Jwt();

  public Jwt getJwt() {
    return jwt;
  }

  public static class Jwt {
    private final Hs256 hs256 = new Hs256();

    public Hs256 getHs256() {
      return hs256;
    }

    public static class Hs256 {
      private boolean enabled = true;
      private String secret;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getSecret() {
        return secret;
      }

      public void setSecret(String secret) {
        this.secret = secret;
      }
    }
  }
}

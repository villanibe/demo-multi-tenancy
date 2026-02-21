package com.demo.multitenancy.security.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class TokenHashing {

  private TokenHashing() {
  }

  public static String sha256Hex(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("raw token is required");
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to hash token", ex);
    }
  }
}

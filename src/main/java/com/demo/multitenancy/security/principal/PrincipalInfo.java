package com.demo.multitenancy.security.principal;

public final class PrincipalInfo {
  private final String subject;
  private final String email;

  public PrincipalInfo(String subject, String email) {
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("subject is required");
    }
    this.subject = subject;
    this.email = email;
  }

  public String getSubject() {
    return subject;
  }

  public String getEmail() {
    return email;
  }
}

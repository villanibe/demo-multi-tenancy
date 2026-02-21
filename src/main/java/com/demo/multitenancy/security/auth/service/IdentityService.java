package com.demo.multitenancy.security.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.demo.multitenancy.security.auth.domain.Identity;
import com.demo.multitenancy.security.auth.domain.IdentityRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {

  private final IdentityRepository identityRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  public IdentityService(IdentityRepository identityRepository, PasswordEncoder passwordEncoder, Clock clock) {
    this.identityRepository = identityRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  public String normalizeEmail(String email) {
    if (email == null) {
      return null;
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }

  public String generateSubject() {
    return "usr_" + UUID.randomUUID();
  }

  @Transactional
  public Identity createIdentity(String desiredSubject, String email, String rawPassword) {
    if (rawPassword == null || rawPassword.isBlank()) {
      throw new IllegalArgumentException("password is required");
    }

    String normalizedEmail = normalizeEmail(email);
    if (normalizedEmail == null || normalizedEmail.isBlank()) {
      throw new IllegalArgumentException("email is required");
    }

    String subject = desiredSubject;
    if (subject == null || subject.isBlank()) {
      subject = generateSubject();
    }

    Instant now = Instant.now(clock);
    String passwordHash = passwordEncoder.encode(rawPassword);
    return identityRepository.save(new Identity(subject, normalizedEmail, passwordHash, now));
  }

  @Transactional
  public Identity ensureIdentityForLogin(String email, String rawPassword) {
    String normalizedEmail = normalizeEmail(email);
    Identity identity = identityRepository.findByEmail(normalizedEmail)
        .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    if (!identity.isEnabled()) {
      throw new IllegalArgumentException("User disabled");
    }

    if (rawPassword == null || rawPassword.isBlank() || !passwordEncoder.matches(rawPassword, identity.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid credentials");
    }

    return identity;
  }

  @Transactional
  public Identity ensureIdentityForInviteAccept(String email, String rawPassword, String desiredSubjectIfCreating) {
    String normalizedEmail = normalizeEmail(email);

    return identityRepository.findByEmail(normalizedEmail)
        .map(existing -> {
          if (!existing.isEnabled()) {
            throw new IllegalArgumentException("User disabled");
          }
          if (rawPassword == null || rawPassword.isBlank() || !passwordEncoder.matches(rawPassword, existing.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
          }
          return existing;
        })
        .orElseGet(() -> createIdentity(desiredSubjectIfCreating, normalizedEmail, rawPassword));
  }

  @Transactional
  public Identity ensureIdentityExists(String email, String rawPassword, String desiredSubjectIfCreating) {
    String normalizedEmail = normalizeEmail(email);

    return identityRepository.findByEmail(normalizedEmail)
        .map(existing -> {
          if (!existing.isEnabled()) {
            throw new IllegalArgumentException("User disabled");
          }
          if (rawPassword == null || rawPassword.isBlank() || !passwordEncoder.matches(rawPassword, existing.getPasswordHash())) {
            throw new IllegalArgumentException("Email already registered");
          }
          return existing;
        })
        .orElseGet(() -> createIdentity(desiredSubjectIfCreating, normalizedEmail, rawPassword));
  }
}

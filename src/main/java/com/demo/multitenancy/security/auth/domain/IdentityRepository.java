package com.demo.multitenancy.security.auth.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityRepository extends JpaRepository<Identity, UUID> {
  Optional<Identity> findByEmail(String email);

  Optional<Identity> findBySubject(String subject);
}

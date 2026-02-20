package com.demo.multitenancy.user.service;

import java.util.Optional;
import java.util.UUID;

import com.demo.multitenancy.user.domain.UserAccount;
import com.demo.multitenancy.user.domain.UserAccountRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserAccountRepository repository;
  private final TenantGuard tenantGuard;

  public UserService(UserAccountRepository repository, TenantGuard tenantGuard) {
    this.repository = repository;
    this.tenantGuard = tenantGuard;
  }

  @Transactional
  public UserAccount createUser(String email, String displayName) {
    String tenantId = tenantGuard.requireTenantId();

    Optional<UserAccount> existing = repository.findByTenantIdAndEmail(tenantId, email);
    if (existing.isPresent()) {
      throw new IllegalArgumentException("User already exists for this tenant: " + email);
    }

    return repository.save(new UserAccount(email, displayName));
  }

  @Transactional(readOnly = true)
  public Optional<UserAccount> findById(UUID id) {
    String tenantId = tenantGuard.requireTenantId();
    return repository.findByTenantIdAndId(tenantId, id);
  }

  @Transactional(readOnly = true)
  public Optional<UserAccount> findByEmail(String email) {
    String tenantId = tenantGuard.requireTenantId();
    return repository.findByTenantIdAndEmail(tenantId, email);
  }
}

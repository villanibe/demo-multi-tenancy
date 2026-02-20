package com.demo.multitenancy.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.demo.multitenancy.user.domain.UserAccount;
import com.demo.multitenancy.user.domain.UserAccountRepository;

import org.junit.jupiter.api.Test;

class UserServiceTest {

  @Test
  void createUser_givenNewEmail_thenSavesUser() {
    UserAccountRepository repository = mock(UserAccountRepository.class);
    TenantGuard tenantGuard = mock(TenantGuard.class);

    when(tenantGuard.requireTenantId()).thenReturn("tenant-a");
    when(repository.findByTenantIdAndEmail("tenant-a", "a@b.com")).thenReturn(Optional.empty());
    when(repository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

    UserService service = new UserService(repository, tenantGuard);

    UserAccount created = service.createUser("a@b.com", "Alice");

    // given
    verify(tenantGuard).requireTenantId();
    // when
    verify(repository).save(any(UserAccount.class));
    // then
    assertThat(created.getEmail()).isEqualTo("a@b.com");
    assertThat(created.getDisplayName()).isEqualTo("Alice");
  }

  @Test
  void createUser_givenExistingEmail_thenThrows() {
    UserAccountRepository repository = mock(UserAccountRepository.class);
    TenantGuard tenantGuard = mock(TenantGuard.class);

    when(tenantGuard.requireTenantId()).thenReturn("tenant-a");
    when(repository.findByTenantIdAndEmail("tenant-a", "a@b.com")).thenReturn(Optional.of(new UserAccount("a@b.com", "Alice")));

    UserService service = new UserService(repository, tenantGuard);

    assertThatThrownBy(() -> service.createUser("a@b.com", "Alice"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");

    verify(repository, never()).save(any());
  }
}

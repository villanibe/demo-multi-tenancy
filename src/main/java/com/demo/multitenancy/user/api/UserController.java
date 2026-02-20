package com.demo.multitenancy.user.api;

import java.util.UUID;

import com.demo.multitenancy.user.domain.UserAccount;
import com.demo.multitenancy.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @Operation(summary = "Create a tenant-scoped user")
  @PreAuthorize("hasAuthority('SCOPE_admin') or hasRole('ADMIN')")
  @PostMapping
  public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
    UserAccount created = userService.createUser(request.getEmail(), request.getDisplayName());
    return ResponseEntity.ok(toResponse(created));
  }

  @Operation(summary = "Get user by id")
  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
    return userService.findById(id)
        .map(user -> ResponseEntity.ok(toResponse(user)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(summary = "Get user by email")
  @GetMapping
  public ResponseEntity<UserResponse> getByEmail(@RequestParam("email") String email) {
    return userService.findByEmail(email)
        .map(user -> ResponseEntity.ok(toResponse(user)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private static UserResponse toResponse(UserAccount user) {
    UserResponse response = new UserResponse();
    response.setId(user.getId());
    response.setEmail(user.getEmail());
    response.setDisplayName(user.getDisplayName());
    response.setEnabled(user.isEnabled());
    return response;
  }
}

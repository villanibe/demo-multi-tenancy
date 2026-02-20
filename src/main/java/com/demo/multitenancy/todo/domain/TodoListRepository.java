package com.demo.multitenancy.todo.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoListRepository extends JpaRepository<TodoList, UUID> {
  List<TodoList> findAllByTenantIdOrderByCreatedAtAsc(String tenantId);

  Optional<TodoList> findByTenantIdAndId(String tenantId, UUID id);
}

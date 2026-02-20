package com.demo.multitenancy.todo.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoItemRepository extends JpaRepository<TodoItem, UUID> {
  List<TodoItem> findAllByTenantIdAndTodoListIdOrderByCreatedAtAsc(String tenantId, UUID todoListId);

  Optional<TodoItem> findByTenantIdAndId(String tenantId, UUID id);
}

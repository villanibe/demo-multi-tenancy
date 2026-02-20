package com.demo.multitenancy.todo.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.demo.multitenancy.todo.domain.TodoItem;
import com.demo.multitenancy.todo.domain.TodoItemRepository;
import com.demo.multitenancy.todo.domain.TodoList;
import com.demo.multitenancy.todo.domain.TodoListRepository;
import com.demo.multitenancy.user.service.TenantGuard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodoService {
  private final TodoListRepository todoListRepository;
  private final TodoItemRepository todoItemRepository;
  private final TenantGuard tenantGuard;
  private final Clock clock;

  public TodoService(
      TodoListRepository todoListRepository,
      TodoItemRepository todoItemRepository,
      TenantGuard tenantGuard,
      Clock clock) {
    this.todoListRepository = todoListRepository;
    this.todoItemRepository = todoItemRepository;
    this.tenantGuard = tenantGuard;
    this.clock = clock;
  }

  @Transactional
  public TodoList createList(String name) {
    tenantGuard.requireTenantId();
    Instant now = Instant.now(clock);
    return todoListRepository.save(new TodoList(name, now));
  }

  @Transactional(readOnly = true)
  public List<TodoList> getLists() {
    String tenantId = tenantGuard.requireTenantId();
    return todoListRepository.findAllByTenantIdOrderByCreatedAtAsc(tenantId);
  }

  @Transactional
  public TodoItem addItem(UUID todoListId, String title, String description) {
    String tenantId = tenantGuard.requireTenantId();
    todoListRepository.findByTenantIdAndId(tenantId, todoListId)
        .orElseThrow(() -> new IllegalArgumentException("Todo list not found"));

    Instant now = Instant.now(clock);
    return todoItemRepository.save(new TodoItem(todoListId, title, description, now));
  }

  @Transactional(readOnly = true)
  public List<TodoItem> getItems(UUID todoListId) {
    String tenantId = tenantGuard.requireTenantId();
    todoListRepository.findByTenantIdAndId(tenantId, todoListId)
        .orElseThrow(() -> new IllegalArgumentException("Todo list not found"));

    return todoItemRepository.findAllByTenantIdAndTodoListIdOrderByCreatedAtAsc(tenantId, todoListId);
  }

  @Transactional
  public TodoItem updateItem(UUID itemId, String title, String description, Boolean completed) {
    String tenantId = tenantGuard.requireTenantId();
    TodoItem item = todoItemRepository.findByTenantIdAndId(tenantId, itemId)
        .orElseThrow(() -> new IllegalArgumentException("Todo item not found"));

    Instant now = Instant.now(clock);
    if (title != null || description != null) {
      item.updateDetails(title, description, now);
    }
    if (completed != null) {
      item.setCompleted(completed.booleanValue(), now);
    }
    return item;
  }

  @Transactional
  public void deleteItem(UUID itemId) {
    String tenantId = tenantGuard.requireTenantId();
    TodoItem item = todoItemRepository.findByTenantIdAndId(tenantId, itemId)
        .orElseThrow(() -> new IllegalArgumentException("Todo item not found"));
    todoItemRepository.delete(item);
  }
}

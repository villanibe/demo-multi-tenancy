package com.demo.multitenancy.todo.api;

import java.util.List;
import java.util.UUID;

import com.demo.multitenancy.todo.domain.TodoItem;
import com.demo.multitenancy.todo.domain.TodoList;
import com.demo.multitenancy.todo.service.TodoService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/todos")
public class TodoController {
  private final TodoService todoService;

  public TodoController(TodoService todoService) {
    this.todoService = todoService;
  }

  @Operation(summary = "Create a todo list")
  @PreAuthorize("@tenantPermission.has('todo.write')")
  @PostMapping("/lists")
  public ResponseEntity<TodoListResponse> createList(@Valid @RequestBody CreateTodoListRequest request) {
    TodoList created = todoService.createList(request.getName());
    return ResponseEntity.ok(toResponse(created));
  }

  @Operation(summary = "Get todo lists")
  @PreAuthorize("@tenantPermission.has('todo.read')")
  @GetMapping("/lists")
  public ResponseEntity<List<TodoListResponse>> lists() {
    return ResponseEntity.ok(todoService.getLists().stream().map(TodoController::toResponse).toList());
  }

  @Operation(summary = "Add item to list")
  @PreAuthorize("@tenantPermission.has('todo.write')")
  @PostMapping("/lists/{listId}/items")
  public ResponseEntity<TodoItemResponse> addItem(
      @PathVariable UUID listId,
      @Valid @RequestBody CreateTodoItemRequest request) {
    try {
      TodoItem created = todoService.addItem(listId, request.getTitle(), request.getDescription());
      return ResponseEntity.ok(toResponse(created));
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(NOT_FOUND, ex.getMessage());
    }
  }

  @Operation(summary = "Get items for list")
  @PreAuthorize("@tenantPermission.has('todo.read')")
  @GetMapping("/lists/{listId}/items")
  public ResponseEntity<List<TodoItemResponse>> items(@PathVariable UUID listId) {
    try {
      return ResponseEntity.ok(todoService.getItems(listId).stream().map(TodoController::toResponse).toList());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(NOT_FOUND, ex.getMessage());
    }
  }

  @Operation(summary = "Update a todo item")
  @PreAuthorize("@tenantPermission.has('todo.write')")
  @PatchMapping("/items/{itemId}")
  public ResponseEntity<TodoItemResponse> updateItem(
      @PathVariable UUID itemId,
      @Valid @RequestBody UpdateTodoItemRequest request) {
    try {
      TodoItem updated = todoService.updateItem(itemId, request.getTitle(), request.getDescription(), request.getCompleted());
      return ResponseEntity.ok(toResponse(updated));
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(NOT_FOUND, ex.getMessage());
    }
  }

  @Operation(summary = "Delete a todo item")
  @PreAuthorize("@tenantPermission.has('todo.write')")
  @DeleteMapping("/items/{itemId}")
  public ResponseEntity<Void> deleteItem(@PathVariable UUID itemId) {
    try {
      todoService.deleteItem(itemId);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(NOT_FOUND, ex.getMessage());
    }
  }

  private static TodoListResponse toResponse(TodoList list) {
    TodoListResponse response = new TodoListResponse();
    response.setId(list.getId());
    response.setName(list.getName());
    response.setCreatedAt(list.getCreatedAt());
    return response;
  }

  private static TodoItemResponse toResponse(TodoItem item) {
    TodoItemResponse response = new TodoItemResponse();
    response.setId(item.getId());
    response.setTodoListId(item.getTodoListId());
    response.setTitle(item.getTitle());
    response.setDescription(item.getDescription());
    response.setCompleted(item.isCompleted());
    response.setCreatedAt(item.getCreatedAt());
    response.setUpdatedAt(item.getUpdatedAt());
    response.setCompletedAt(item.getCompletedAt());
    return response;
  }

  public static class CreateTodoListRequest {
    @NotBlank
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class TodoListResponse {
    private UUID id;
    private String name;
    private java.time.Instant createdAt;

    public UUID getId() {
      return id;
    }

    public void setId(UUID id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public java.time.Instant getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(java.time.Instant createdAt) {
      this.createdAt = createdAt;
    }
  }

  public static class CreateTodoItemRequest {
    @NotBlank
    private String title;

    private String description;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  public static class UpdateTodoItemRequest {
    private String title;
    private String description;
    private Boolean completed;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Boolean getCompleted() {
      return completed;
    }

    public void setCompleted(Boolean completed) {
      this.completed = completed;
    }
  }

  public static class TodoItemResponse {
    private UUID id;
    private UUID todoListId;
    private String title;
    private String description;
    private boolean completed;
    private java.time.Instant createdAt;
    private java.time.Instant updatedAt;
    private java.time.Instant completedAt;

    public UUID getId() {
      return id;
    }

    public void setId(UUID id) {
      this.id = id;
    }

    public UUID getTodoListId() {
      return todoListId;
    }

    public void setTodoListId(UUID todoListId) {
      this.todoListId = todoListId;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public boolean isCompleted() {
      return completed;
    }

    public void setCompleted(boolean completed) {
      this.completed = completed;
    }

    public java.time.Instant getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(java.time.Instant createdAt) {
      this.createdAt = createdAt;
    }

    public java.time.Instant getUpdatedAt() {
      return updatedAt;
    }

    public void setUpdatedAt(java.time.Instant updatedAt) {
      this.updatedAt = updatedAt;
    }

    public java.time.Instant getCompletedAt() {
      return completedAt;
    }

    public void setCompletedAt(java.time.Instant completedAt) {
      this.completedAt = completedAt;
    }
  }
}

package com.demo.multitenancy.todo.domain;

import java.time.Instant;
import java.util.UUID;

import com.demo.multitenancy.shared.jpa.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "todo_items")
public class TodoItem extends TenantScopedEntity {

  @Column(name = "todo_list_id", nullable = false)
  private UUID todoListId;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description")
  private String description;

  @Column(name = "completed", nullable = false)
  private boolean completed;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  protected TodoItem() {
  }

  public TodoItem(UUID todoListId, String title, String description, Instant createdAt) {
    this.todoListId = todoListId;
    this.title = title;
    this.description = description;
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
    this.completed = false;
  }

  public UUID getTodoListId() {
    return todoListId;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public boolean isCompleted() {
    return completed;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void updateDetails(String title, String description, Instant now) {
    if (title != null && !title.isBlank()) {
      this.title = title;
    }
    this.description = description;
    this.updatedAt = now;
  }

  public void setCompleted(boolean completed, Instant now) {
    this.completed = completed;
    this.updatedAt = now;
    this.completedAt = completed ? now : null;
  }
}

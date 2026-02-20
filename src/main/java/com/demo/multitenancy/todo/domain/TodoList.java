package com.demo.multitenancy.todo.domain;

import java.time.Instant;

import com.demo.multitenancy.shared.jpa.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "todo_lists",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_todo_lists_tenant_name", columnNames = { "tenant_id", "name" })
    })
public class TodoList extends TenantScopedEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected TodoList() {
  }

  public TodoList(String name, Instant createdAt) {
    this.name = name;
    this.createdAt = createdAt;
  }

  public String getName() {
    return name;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

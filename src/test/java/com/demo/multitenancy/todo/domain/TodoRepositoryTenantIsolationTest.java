package com.demo.multitenancy.todo.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.demo.multitenancy.tenant.TenantConfiguration;
import com.demo.multitenancy.tenant.TenantContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@Import(TenantConfiguration.class)
class TodoRepositoryTenantIsolationTest {

  @Autowired
  private TodoListRepository todoListRepository;

  @Autowired
  private TodoItemRepository todoItemRepository;

  @Test
  void lists_givenSameNameDifferentTenants_thenIsolationWorks() {
    try {
      TenantContext.setTenantId("tenant-a");
      todoListRepository.save(new TodoList("Backlog", Instant.parse("2026-02-20T00:00:00Z")));
      todoListRepository.flush();

      TenantContext.setTenantId("tenant-b");
      todoListRepository.save(new TodoList("Backlog", Instant.parse("2026-02-20T00:00:01Z")));
      todoListRepository.flush();

      List<TodoList> aLists = todoListRepository.findAllByTenantIdOrderByCreatedAtAsc("tenant-a");
      List<TodoList> bLists = todoListRepository.findAllByTenantIdOrderByCreatedAtAsc("tenant-b");

      assertThat(aLists).hasSize(1);
      assertThat(aLists.get(0).getTenantId()).isEqualTo("tenant-a");
      assertThat(aLists.get(0).getName()).isEqualTo("Backlog");

      assertThat(bLists).hasSize(1);
      assertThat(bLists.get(0).getTenantId()).isEqualTo("tenant-b");
      assertThat(bLists.get(0).getName()).isEqualTo("Backlog");
    } finally {
      TenantContext.clear();
    }
  }

  @Test
  void items_givenSameListIdDifferentTenants_thenIsolationWorks() {
    try {
      UUID sharedListId = UUID.fromString("00000000-0000-0000-0000-000000000001");

      TenantContext.setTenantId("tenant-a");
      TodoItem a1 = todoItemRepository.save(new TodoItem(sharedListId, "A1", null, Instant.parse("2026-02-20T00:00:00Z")));
      todoItemRepository.flush();

      TenantContext.setTenantId("tenant-b");
      TodoItem b1 = todoItemRepository.save(new TodoItem(sharedListId, "B1", null, Instant.parse("2026-02-20T00:00:01Z")));
      todoItemRepository.flush();

      List<TodoItem> aItems = todoItemRepository.findAllByTenantIdAndTodoListIdOrderByCreatedAtAsc("tenant-a", sharedListId);
      List<TodoItem> bItems = todoItemRepository.findAllByTenantIdAndTodoListIdOrderByCreatedAtAsc("tenant-b", sharedListId);

      assertThat(aItems).extracting(TodoItem::getId).containsExactly(a1.getId());
      assertThat(bItems).extracting(TodoItem::getId).containsExactly(b1.getId());
    } finally {
      TenantContext.clear();
    }
  }
}

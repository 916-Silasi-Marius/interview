package com.interview.model.mapper;

import com.interview.model.dto.TaskRequest;
import com.interview.model.dto.TaskResponse;
import com.interview.model.dto.TaskUpdateRequest;
import com.interview.model.entities.Employee;
import com.interview.model.entities.Tag;
import com.interview.model.entities.Task;
import com.interview.model.enums.TaskPriority;
import com.interview.model.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.interview.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

class TaskMapperTest {

    @Test
    void toResponse_withReporterAndAssigneeAndTags_mapsAll() {
        Employee reporter = buildEmployee(1L, "reporter");
        Employee assignee = buildEmployee(2L, "assignee");
        Tag tag1 = buildTag(1L, "bug", null);
        Tag tag2 = buildTag(2L, "feature", null);
        Task task = Task.builder()
                .id(1L)
                .taskKey("PROJ-1")
                .title("Title")
                .description("Desc")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .storyPoints(5)
                .reporter(reporter)
                .assignee(assignee)
                .tags(new HashSet<>(Set.of(tag1, tag2)))
                .createdAt(FIXED_CREATED_AT)
                .updatedAt(FIXED_UPDATED_AT)
                .build();

        TaskResponse response = TaskMapper.toResponse(task);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.reporterId()).isEqualTo(1L);
        assertThat(response.reporterName()).isEqualTo("Full reporter");
        assertThat(response.assigneeId()).isEqualTo(2L);
        assertThat(response.assigneeName()).isEqualTo("Full assignee");
        assertThat(response.tags()).containsExactlyInAnyOrder("bug", "feature");
    }

    @Test
    void toResponse_nullReporterAndAssignee_mapsToNull() {
        Task task = Task.builder()
                .id(1L).taskKey("K")
                .title("T").status(TaskStatus.TODO)
                .priority(TaskPriority.LOW)
                .reporter(null)
                .assignee(null)
                .tags(new HashSet<>())
                .createdAt(FIXED_CREATED_AT)
                .updatedAt(FIXED_UPDATED_AT)
                .build();

        TaskResponse response = TaskMapper.toResponse(task);

        assertThat(response.reporterId()).isNull();
        assertThat(response.reporterName()).isNull();
        assertThat(response.assigneeId()).isNull();
        assertThat(response.assigneeName()).isNull();
        assertThat(response.tags()).isEmpty();
    }

    @Test
    void toEntity_withDefaults_setsStatusTodoAndPriorityMedium() {
        TaskRequest request = new TaskRequest("K", "T", null, null, null, null, null, null, null);
        Employee reporter = buildEmployee(1L, "reporter");

        Task entity = TaskMapper.toEntity(request, reporter, null, null);

        assertThat(entity.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(entity.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(entity.getTags()).isEmpty();
    }

    @Test
    void toEntity_withExplicitStatusAndPriority_setsProvided() {
        TaskRequest request = new TaskRequest("K", "T", "D", TaskStatus.DONE, TaskPriority.URGENT, 13, null, null, null);
        Employee reporter = buildEmployee(1L, "reporter");
        Set<Tag> tags = Set.of(buildTag());

        Task entity = TaskMapper.toEntity(request, reporter, null, tags);

        assertThat(entity.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(entity.getPriority()).isEqualTo(TaskPriority.URGENT);
        assertThat(entity.getStoryPoints()).isEqualTo(13);
        assertThat(entity.getTags()).hasSize(1);
    }

    @Test
    void fullUpdateEntity_overwritesScalarFields() {
        Task task = Task.builder()
                .id(1L)
                .taskKey("OLD")
                .title("Old")
                .description("Old desc")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.LOW)
                .storyPoints(1)
                .createdAt(FIXED_CREATED_AT)
                .updatedAt(FIXED_UPDATED_AT)
                .build();
        TaskRequest request = new TaskRequest("NEW", "New Title", "New desc", TaskStatus.DONE, TaskPriority.URGENT, 8, null, null, null);

        TaskMapper.fullUpdateEntity(task, request);

        assertThat(task.getTaskKey()).isEqualTo("NEW");
        assertThat(task.getTitle()).isEqualTo("New Title");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.URGENT);
        assertThat(task.getStoryPoints()).isEqualTo(8);
    }

    @Test
    void fullUpdateEntity_nullStatusAndPriority_defaultsApplied() {
        Task task = Task.builder()
                .id(1L)
                .taskKey("K")
                .title("T")
                .status(TaskStatus.DONE)
                .priority(TaskPriority.URGENT)
                .createdAt(FIXED_CREATED_AT)
                .updatedAt(FIXED_UPDATED_AT)
                .build();
        TaskRequest request = new TaskRequest("K", "T", null, null, null, null, null, null, null);

        TaskMapper.fullUpdateEntity(task, request);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
    }

    @Test
    void patchEntity_nullFields_doesNotOverwrite() {
        Task task = Task.builder()
                .id(1L)
                .taskKey("PROJ-1")
                .title("Title")
                .description("Desc")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .storyPoints(5)
                .createdAt(FIXED_CREATED_AT)
                .updatedAt(FIXED_UPDATED_AT)
                .build();
        TaskUpdateRequest request = new TaskUpdateRequest(null, null, null, null, null, null, null, null, null);

        TaskMapper.patchEntity(task, request);

        assertThat(task.getTaskKey()).isEqualTo("PROJ-1");
        assertThat(task.getTitle()).isEqualTo("Title");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void patchEntity_allFields_overwritesAll() {
        Task task = Task.builder()
                .id(1L)
                .taskKey("OLD")
                .title("Old")
                .description("Old")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.LOW)
                .storyPoints(1)
                .createdAt(FIXED_CREATED_AT)
                .updatedAt(FIXED_UPDATED_AT)
                .build();
        TaskUpdateRequest request = new TaskUpdateRequest("NEW", "New", "New desc", TaskStatus.DONE, TaskPriority.URGENT, 13, null, null, null);

        TaskMapper.patchEntity(task, request);

        assertThat(task.getTaskKey()).isEqualTo("NEW");
        assertThat(task.getTitle()).isEqualTo("New");
        assertThat(task.getDescription()).isEqualTo("New desc");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.URGENT);
        assertThat(task.getStoryPoints()).isEqualTo(13);
    }
}

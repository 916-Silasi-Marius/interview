package com.interview.service;

import com.interview.exception.ConcurrentModificationException;
import com.interview.exception.DuplicateResourceException;
import com.interview.exception.ResourceNotFoundException;
import com.interview.exception.TaskAlreadyAssignedException;
import com.interview.exception.TaskNotAssignedException;
import com.interview.model.dto.TaskRequest;
import com.interview.model.dto.TaskResponse;
import com.interview.model.dto.TaskStatusRequest;
import com.interview.model.dto.TaskUpdateRequest;
import com.interview.model.entities.Employee;
import com.interview.model.entities.Tag;
import com.interview.model.entities.Task;
import com.interview.model.enums.TaskPriority;
import com.interview.model.enums.TaskStatus;
import com.interview.repository.EmployeeRepository;
import com.interview.repository.TagRepository;
import com.interview.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.*;

import static com.interview.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TaskService taskService;

    private void assertAllFields(TaskResponse response, Task expected) {
        assertThat(response.id()).isEqualTo(expected.getId());
        assertThat(response.taskKey()).isEqualTo(expected.getTaskKey());
        assertThat(response.title()).isEqualTo(expected.getTitle());
        assertThat(response.description()).isEqualTo(expected.getDescription());
        assertThat(response.status()).isEqualTo(expected.getStatus());
        assertThat(response.priority()).isEqualTo(expected.getPriority());
        assertThat(response.storyPoints()).isEqualTo(expected.getStoryPoints());
        assertThat(response.reporterId()).isEqualTo(expected.getReporter() != null ? expected.getReporter().getId() : null);
        assertThat(response.reporterName()).isEqualTo(expected.getReporter() != null ? expected.getReporter().getFullName() : null);
        assertThat(response.assigneeId()).isEqualTo(expected.getAssignee() != null ? expected.getAssignee().getId() : null);
        assertThat(response.assigneeName()).isEqualTo(expected.getAssignee() != null ? expected.getAssignee().getFullName() : null);
        assertThat(response.tags()).isEqualTo(
                expected.getTags() != null
                        ? expected.getTags().stream().map(Tag::getName).collect(java.util.stream.Collectors.toSet())
                        : Collections.emptySet());
        assertThat(response.createdAt()).isEqualTo(expected.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(expected.getUpdatedAt());
    }

    @Test
    void getAllTasks_returnsPageOfResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Employee reporter = buildEmployee();
        Task task = buildTask(reporter, null);
        when(taskRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(task), pageable, 1));
        when(taskRepository.findAllWithRelationsByIdIn(List.of(1L))).thenReturn(List.of(task));

        Page<TaskResponse> result = taskService.getAllTasks(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertAllFields(result.getContent().getFirst(), task);
    }

    @Test
    void getTaskById_found_returnsResponse() {
        Task task = buildTask(buildEmployee(1L, "reporter"), null);
        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.getTaskById(1L);

        assertAllFields(response, task);
    }

    @Test
    void getTaskById_notFound_throwsResourceNotFoundException() {
        when(taskRepository.findWithRelationsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchTasks_returnsFilteredPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Employee reporter = buildEmployee(1L, "reporter");
        Task task = buildTask(reporter, null);
        when(taskRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(task)));
        when(taskRepository.findAllWithRelationsByIdIn(List.of(1L))).thenReturn(List.of(task));

        Page<TaskResponse> result = taskService.searchTasks("login", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertAllFields(result.getContent().getFirst(), task);
    }

    @Test
    void createTask_withExplicitReporter_success() {
        Employee reporter = buildEmployee(1L, "reporter");
        Employee assignee = buildEmployee(2L, "assignee");
        Tag tag = buildTag();
        TaskRequest request = new TaskRequest("NEW-1", "New Task", "Desc", TaskStatus.TODO, TaskPriority.HIGH, 5, 1L, 2L, Set.of(1L));

        when(taskRepository.existsByTaskKey("NEW-1")).thenReturn(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(tagRepository.findAllById(Set.of(1L))).thenReturn(List.of(tag));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            t.setCreatedAt(FIXED_CREATED_AT);
            t.setUpdatedAt(FIXED_UPDATED_AT);
            return t;
        });

        TaskResponse response = taskService.createTask(request, "reporter");

        Task expected = Task.builder()
                .id(1L)
                .taskKey("NEW-1")
                .title("New Task")
                .description("Desc")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .storyPoints(5)
                .reporter(reporter)
                .assignee(assignee)
                .tags(new HashSet<>(Set.of(tag)))
                .createdAt(FIXED_CREATED_AT)
                .updatedAt(FIXED_UPDATED_AT)
                .build();
        assertAllFields(response, expected);
    }

    @Test
    void createTask_withImplicitReporter_usesJwtUsername() {
        Employee reporter = buildEmployee(1L, "jdoe");
        TaskRequest request = new TaskRequest("NEW-1", "New Task", null, null, null, null, null, null, null);

        when(taskRepository.existsByTaskKey("NEW-1")).thenReturn(false);
        when(employeeRepository.findByUsername("jdoe")).thenReturn(Optional.of(reporter));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            t.setCreatedAt(FIXED_CREATED_AT);
            t.setUpdatedAt(FIXED_UPDATED_AT);
            return t;
        });

        TaskResponse response = taskService.createTask(request, "jdoe");

        Task expected = Task.builder()
                .id(1L)
                .taskKey("NEW-1")
                .title("New Task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .reporter(reporter)
                .tags(Collections.emptySet())
                .createdAt(FIXED_CREATED_AT)
                .updatedAt(FIXED_UPDATED_AT)
                .build();
        assertAllFields(response, expected);
        verify(employeeRepository).findByUsername("jdoe");
    }

    @Test
    void createTask_duplicateTaskKey_throwsDuplicateResourceException() {
        TaskRequest request = new TaskRequest("PROJ-1", "Title", null, null, null, null, null, null, null);
        when(taskRepository.existsByTaskKey("PROJ-1")).thenReturn(true);

        assertThatThrownBy(() -> taskService.createTask(request, "user"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("PROJ-1");
    }

    @Test
    void createTask_reporterNotFound_throwsResourceNotFoundException() {
        TaskRequest request = new TaskRequest("NEW-1", "Title", null, null, null, null, 99L, null, null);
        when(taskRepository.existsByTaskKey("NEW-1")).thenReturn(false);
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(request, "user"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reporter");
    }

    @Test
    void createTask_invalidTagIds_throwsResourceNotFoundException() {
        Employee reporter = buildEmployee(1L, "reporter");
        TaskRequest request = new TaskRequest("NEW-1", "Title", null, null, null, null, 1L, null, Set.of(1L, 2L));
        when(taskRepository.existsByTaskKey("NEW-1")).thenReturn(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(tagRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(buildTag()));

        assertThatThrownBy(() -> taskService.createTask(request, "user"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("tag");
    }

    @Test
    void updateTask_success_returnsUpdatedResponse() {
        Employee reporter = buildEmployee(1L, "reporter");
        Task task = buildTask(reporter, null);
        TaskRequest request = new TaskRequest("PROJ-1", "Updated", "New desc", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, 8, 1L, null, null);

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(reporter));

        TaskResponse response = taskService.updateTask(1L, request);

        assertAllFields(response, task);
    }

    @Test
    void updateTask_notFound_throwsResourceNotFoundException() {
        TaskRequest request = new TaskRequest("K", "T", null, null, null, null, null, null, null);
        when(taskRepository.findWithRelationsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateTask_duplicateTaskKey_throwsDuplicateResourceException() {
        Employee reporter = buildEmployee(1L, "reporter");
        Task task = buildTask(reporter, null);
        TaskRequest request = new TaskRequest("TAKEN", "Title", null, null, null, null, null, null, null);

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.existsByTaskKey("TAKEN")).thenReturn(true);

        assertThatThrownBy(() -> taskService.updateTask(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("TAKEN");
    }

    @Test
    void updateTask_sameTaskKey_noConflict() {
        Employee reporter = buildEmployee(1L, "reporter");
        Task task = buildTask(reporter, null);
        TaskRequest request = new TaskRequest("PROJ-1", "Updated", null, null, null, null, null, null, null);

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.updateTask(1L, request);

        assertAllFields(response, task);
        verify(taskRepository, never()).existsByTaskKey(anyString());
    }

    @Test
    void patchTask_success_appliesOnlyProvidedFields() {
        Employee reporter = buildEmployee(1L, "reporter");
        Task task = buildTask(reporter, null);
        TaskUpdateRequest request = new TaskUpdateRequest(null, "Patched Title", null, null, null, null, null, null, null);

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.patchTask(1L, request);

        assertAllFields(response, task);
    }

    @Test
    void patchTask_duplicateTaskKey_throwsDuplicateResourceException() {
        Employee reporter = buildEmployee(1L, "reporter");
        Task task = buildTask(reporter, null);
        TaskUpdateRequest request = new TaskUpdateRequest("TAKEN", null, null, null, null, null, null, null, null);

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.existsByTaskKey("TAKEN")).thenReturn(true);

        assertThatThrownBy(() -> taskService.patchTask(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("TAKEN");
    }

    @Test
    void selfUpdateTaskStatus_success_updatesStatus() {
        Employee reporter = buildEmployee(1L, "reporter");
        Employee assignee = buildEmployee(2L, "dev");
        Task task = buildTask(reporter, assignee);
        TaskStatusRequest request = new TaskStatusRequest(TaskStatus.IN_PROGRESS);

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));
        when(employeeRepository.findByUsername("dev")).thenReturn(Optional.of(assignee));

        TaskResponse response = taskService.selfUpdateTaskStatus(1L, request, "dev");

        assertAllFields(response, task);
    }

    @Test
    void selfUpdateTaskStatus_notAssigned_throwsTaskNotAssignedException() {
        Employee reporter = buildEmployee(1L, "reporter");
        Employee otherEmployee = buildEmployee(3L, "other");
        Task task = buildTask(reporter, buildEmployee(2L, "assignee"));
        TaskStatusRequest request = new TaskStatusRequest(TaskStatus.DONE);

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));
        when(employeeRepository.findByUsername("other")).thenReturn(Optional.of(otherEmployee));

        assertThatThrownBy(() -> taskService.selfUpdateTaskStatus(1L, request, "other"))
                .isInstanceOf(TaskNotAssignedException.class)
                .hasMessageContaining("1");
    }

    @Test
    void selfUpdateTaskStatus_assigneeIsNull_throwsTaskNotAssignedException() {
        Task task = buildTask(buildEmployee(1L, "reporter"), null);
        Employee employee = buildEmployee(2L, "dev");
        TaskStatusRequest request = new TaskStatusRequest(TaskStatus.DONE);

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));
        when(employeeRepository.findByUsername("dev")).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> taskService.selfUpdateTaskStatus(1L, request, "dev"))
                .isInstanceOf(TaskNotAssignedException.class)
                .hasMessageContaining("1");
    }

    @Test
    void selfAssignTask_unassigned_success() {
        Employee reporter = buildEmployee(1L, "reporter");
        Task task = buildTask(reporter, null);
        Employee assignee = buildEmployee(2L, "dev");

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));
        when(employeeRepository.findByUsername("dev")).thenReturn(Optional.of(assignee));

        TaskResponse response = taskService.selfAssignTask(1L, "dev");

        assertAllFields(response, task);
    }

    @Test
    void selfAssignTask_alreadySelfAssigned_succeeds() {
        Employee reporter = buildEmployee(1L, "reporter");
        Employee dev = buildEmployee(2L, "dev");
        Task task = buildTask(reporter, dev);

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));
        when(employeeRepository.findByUsername("dev")).thenReturn(Optional.of(dev));

        TaskResponse response = taskService.selfAssignTask(1L, "dev");

        assertAllFields(response, task);
    }

    @Test
    void selfAssignTask_assignedToOther_throwsTaskAlreadyAssignedException() {
        Employee other = buildEmployee(3L, "other");
        Task task = buildTask(buildEmployee(1L, "reporter"), other);

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.selfAssignTask(1L, "dev"))
                .isInstanceOf(TaskAlreadyAssignedException.class)
                .hasMessageContaining("Full other");
    }

    @Test
    void deleteTask_exists_deletesSuccessfully() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.deleteTask(1L);

        verify(taskRepository).deleteById(1L);
    }

    @Test
    void deleteTask_notFound_throwsResourceNotFoundException() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createTask_concurrentDuplicateKey_throwsDuplicateResourceException() {
        Employee reporter = buildEmployee(1L, "reporter");
        TaskRequest request = new TaskRequest("NEW-1", "Title", null, null, null, null, 1L, null, null);

        when(taskRepository.existsByTaskKey("NEW-1")).thenReturn(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(taskRepository.save(any(Task.class))).thenThrow(new DataIntegrityViolationException("Unique constraint"));

        assertThatThrownBy(() -> taskService.createTask(request, "reporter"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("NEW-1");
    }

    @Test
    void updateTask_concurrentModification_throwsConcurrentModificationException() {
        Employee reporter = buildEmployee(1L, "reporter");
        Task task = buildTask(reporter, null);
        TaskRequest request = new TaskRequest("PROJ-1", "Updated", null, null, null, null, null, null, null);

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));
        doThrow(new ObjectOptimisticLockingFailureException(Task.class, 1L))
                .when(taskRepository).flush();

        assertThatThrownBy(() -> taskService.updateTask(1L, request))
                .isInstanceOf(ConcurrentModificationException.class)
                .hasMessageContaining("Task with id 1")
                .hasMessageContaining("Please retry");
    }

    @Test
    void patchTask_concurrentModification_throwsConcurrentModificationException() {
        Employee reporter = buildEmployee(1L, "reporter");
        Task task = buildTask(reporter, null);
        TaskUpdateRequest request = new TaskUpdateRequest(null, "Patched", null, null, null, null, null, null, null);

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));
        doThrow(new ObjectOptimisticLockingFailureException(Task.class, 1L))
                .when(taskRepository).flush();

        assertThatThrownBy(() -> taskService.patchTask(1L, request))
                .isInstanceOf(ConcurrentModificationException.class)
                .hasMessageContaining("Task with id 1")
                .hasMessageContaining("Please retry");
    }

    @Test
    void selfUpdateTaskStatus_concurrentModification_throwsConcurrentModificationException() {
        Employee reporter = buildEmployee(1L, "reporter");
        Employee assignee = buildEmployee(2L, "dev");
        Task task = buildTask(reporter, assignee);
        TaskStatusRequest request = new TaskStatusRequest(TaskStatus.DONE);

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));
        when(employeeRepository.findByUsername("dev")).thenReturn(Optional.of(assignee));
        doThrow(new ObjectOptimisticLockingFailureException(Task.class, 1L))
                .when(taskRepository).flush();

        assertThatThrownBy(() -> taskService.selfUpdateTaskStatus(1L, request, "dev"))
                .isInstanceOf(ConcurrentModificationException.class)
                .hasMessageContaining("Task with id 1")
                .hasMessageContaining("Please retry");
    }

    @Test
    void selfAssignTask_concurrentModification_throwsConcurrentModificationException() {
        Employee reporter = buildEmployee(1L, "reporter");
        Task task = buildTask(reporter, null);
        Employee assignee = buildEmployee(2L, "dev");

        when(taskRepository.findWithRelationsById(1L)).thenReturn(Optional.of(task));
        when(employeeRepository.findByUsername("dev")).thenReturn(Optional.of(assignee));
        doThrow(new ObjectOptimisticLockingFailureException(Task.class, 1L))
                .when(taskRepository).flush();

        assertThatThrownBy(() -> taskService.selfAssignTask(1L, "dev"))
                .isInstanceOf(ConcurrentModificationException.class)
                .hasMessageContaining("Task with id 1")
                .hasMessageContaining("Please retry");
    }
}

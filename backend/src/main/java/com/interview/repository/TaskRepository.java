package com.interview.repository;

import com.interview.model.entities.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Task} entities.
 *
 * <p>Provides CRUD operations, specification-based queries, and custom
 * query methods for looking up tasks by their unique key and checking for duplicates.</p>
 *
 * <p>Paginated list queries use a <strong>two-query approach</strong> to avoid
 * in-memory pagination when fetching collection associations ({@code tags}).
 * The first query fetches the matching tasks with real SQL pagination,
 * and the second query loads the full entities with relations for just those IDs.</p>
 */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /**
     * Checks whether a task with the given task key already exists.
     *
     * @param taskKey the task key to check
     * @return {@code true} if a matching task exists
     */
    boolean existsByTaskKey(String taskKey);

    /**
     * Finds a task by ID with reporter, assignee, and tags eagerly fetched.
     *
     * @param id the task ID
     * @return an optional containing the task with loaded relations, if found
     */
    @EntityGraph("Task.withRelations")
    Optional<Task> findWithRelationsById(Long id);

    /**
     * Loads full task entities with reporter, assignee, and tags eagerly fetched
     * for a given list of IDs.
     *
     * <p>This is the second step of the two-query approach. Since the result set
     * is already bounded by the ID list, there is no in-memory pagination.</p>
     *
     * @param ids the task IDs to load
     * @return the tasks with all relations loaded
     */
    @EntityGraph("Task.withRelations")
    @Query("SELECT t FROM Task t WHERE t.id IN :ids")
    List<Task> findAllWithRelationsByIdIn(@Param("ids") List<Long> ids);
}

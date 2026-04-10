package com.interview.repository;

import com.interview.model.entities.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Task} entities.
 *
 * <p>Provides CRUD operations, specification-based queries, and custom
 * query methods for looking up tasks by their unique key and checking for duplicates.</p>
 *
 * <p>Methods annotated with {@code @EntityGraph("Task.withRelations")} eagerly fetch
 * the {@code reporter}, {@code assignee}, and {@code tags} associations in a single
 * query to avoid the N+1 problem.</p>
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
     * Retrieves a paginated list of all tasks with relations eagerly fetched.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of tasks with loaded relations
     */
    @EntityGraph("Task.withRelations")
    Page<Task> findAllWithRelationsBy(Pageable pageable);

    /**
     * Retrieves a paginated list of tasks matching a specification,
     * with relations eagerly fetched.
     *
     * @param spec     the specification to filter by
     * @param pageable pagination and sorting parameters
     * @return a page of matching tasks with loaded relations
     */
    @EntityGraph("Task.withRelations")
    Page<Task> findAll(Specification<Task> spec, Pageable pageable);
}


package com.interview.repository;

import com.interview.model.entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data JPA repository for {@link Task} entities.
 *
 * <p>Provides CRUD operations, specification-based queries, and custom
 * query methods for looking up tasks by their unique key and checking for duplicates.</p>
 */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /**
     * Checks whether a task with the given task key already exists.
     *
     * @param taskKey the task key to check
     * @return {@code true} if a matching task exists
     */
    boolean existsByTaskKey(String taskKey);
}


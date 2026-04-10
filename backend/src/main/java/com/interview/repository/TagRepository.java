package com.interview.repository;

import com.interview.model.entities.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Tag} entities.
 *
 * <p>Provides CRUD operations for tags used in task labeling.</p>
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * Checks whether a tag with the given name already exists.
     *
     * @param name the tag name to check
     * @return {@code true} if a matching tag exists
     */
    boolean existsByName(String name);
}


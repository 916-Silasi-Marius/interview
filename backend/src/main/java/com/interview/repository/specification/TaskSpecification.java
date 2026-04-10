package com.interview.repository.specification;

import com.interview.model.entities.Task;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA {@link Specification} factory for building dynamic task search queries.
 *
 * <p>Provides reusable, composable specifications that can be combined
 * for flexible filtering without writing custom JPQL.</p>
 */
public class TaskSpecification {

    private TaskSpecification() {
    }

    /**
     * Builds a specification that matches tasks where the title or description
     * contains <strong>any</strong> of the words in the given query string.
     *
     * <p>The query is split by whitespace into individual keywords. Each keyword
     * is matched as a case-insensitive substring against both {@code title} and
     * {@code description}. A task is included if it matches <em>at least one</em>
     * keyword in either field.</p>
     *
     * <p>Example: the query {@code "Login feature implementation"} is split into
     * {@code ["login", "feature", "implementation"]}. A task titled
     * {@code "Implement login page"} matches because it contains {@code "login"}
     * and {@code "implement"} is a substring of {@code "implementation"}.</p>
     *
     * @param query the search string containing one or more keywords
     * @return a specification that matches tasks containing any of the keywords
     */
    public static Specification<Task> titleOrDescriptionContainsAnyWord(String query) {
        return (root, criteriaQuery, cb) -> {
            String[] words = query.trim().split("\\s+");
            List<Predicate> predicates = new ArrayList<>();

            for (String word : words) {
                String pattern = "%" + word.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}


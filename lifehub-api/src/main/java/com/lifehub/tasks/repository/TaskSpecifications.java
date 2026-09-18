package com.lifehub.tasks.repository;

import com.lifehub.tasks.entity.Task;
import com.lifehub.tasks.entity.TaskType;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

/**
 * Optional-filter predicates built only for non-null inputs — see
 * finance/repository/TransactionSpecifications and TROUBLESHOOTING.md for why a single JPQL query
 * with "(:param IS NULL OR field = :param)" is avoided here (PostgreSQL can't infer that parameter's
 * type from an IS NULL-only comparison).
 */
public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> userIdEquals(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<Task> typeEquals(TaskType type) {
        return type == null ? null : (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Task> isCompletedEquals(Boolean isCompleted) {
        return isCompleted == null ? null : (root, query, cb) -> cb.equal(root.get("isCompleted"), isCompleted);
    }

    public static Specification<Task> dueAtFrom(LocalDateTime from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueAt"), from);
    }

    public static Specification<Task> dueAtTo(LocalDateTime to) {
        return to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dueAt"), to);
    }
}

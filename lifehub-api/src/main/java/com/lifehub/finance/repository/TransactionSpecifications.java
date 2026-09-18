package com.lifehub.finance.repository;

import com.lifehub.finance.entity.Transaction;
import com.lifehub.finance.entity.TransactionType;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Optional-filter predicates built only for non-null inputs. Deliberately not a single JPQL query
 * with "(:param IS NULL OR field = :param)": PostgreSQL cannot infer a bind parameter's type from
 * an "IS NULL" comparison alone, which makes that pattern fail at the JDBC level once a parameter
 * is null in one clause but bound to a real value elsewhere in the same statement.
 */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> userIdEquals(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<Transaction> accountIdEquals(Long accountId) {
        return accountId == null ? null : (root, query, cb) -> cb.equal(root.get("accountId"), accountId);
    }

    public static Specification<Transaction> categoryIdEquals(Long categoryId) {
        return categoryId == null ? null : (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
    }

    public static Specification<Transaction> typeEquals(TransactionType type) {
        return type == null ? null : (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> occurredAtFrom(LocalDate from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    public static Specification<Transaction> occurredAtTo(LocalDate to) {
        return to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to);
    }
}

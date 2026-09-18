package com.lifehub.finance.repository;

import com.lifehub.finance.entity.Transaction;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    boolean existsByAccountId(Long accountId);

    boolean existsByCategoryId(Long categoryId);

    @Query("""
            SELECT new com.lifehub.finance.repository.TypeAmountAggregate(t.type, COALESCE(SUM(t.amount), 0))
            FROM Transaction t
            WHERE t.userId = :userId AND t.occurredAt BETWEEN :from AND :to
            GROUP BY t.type
            """)
    List<TypeAmountAggregate> sumAmountByTypeBetween(@Param("userId") Long userId,
                                                       @Param("from") LocalDate from,
                                                       @Param("to") LocalDate to);

    @Query("""
            SELECT new com.lifehub.finance.repository.CategoryAmountAggregate(t.categoryId, t.type, COALESCE(SUM(t.amount), 0))
            FROM Transaction t
            WHERE t.userId = :userId AND t.occurredAt BETWEEN :from AND :to
            GROUP BY t.categoryId, t.type
            """)
    List<CategoryAmountAggregate> sumAmountByCategoryBetween(@Param("userId") Long userId,
                                                               @Param("from") LocalDate from,
                                                               @Param("to") LocalDate to);
}

package com.lifehub.finance.repository;

import com.lifehub.finance.entity.TransactionCategory;
import com.lifehub.finance.entity.TransactionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, Long> {

    List<TransactionCategory> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<TransactionCategory> findAllByUserIdAndTypeOrderByCreatedAtDesc(Long userId, TransactionType type);

    Optional<TransactionCategory> findByIdAndUserId(Long id, Long userId);
}

package com.lifehub.finance.dto.response;

import com.lifehub.finance.entity.Transaction;
import com.lifehub.finance.entity.TransactionSource;
import com.lifehub.finance.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long accountId,
        Long categoryId,
        BigDecimal amount,
        TransactionType type,
        String memo,
        LocalDate occurredAt,
        TransactionSource source,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getCategoryId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getMemo(),
                transaction.getOccurredAt(),
                transaction.getSource(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt());
    }
}

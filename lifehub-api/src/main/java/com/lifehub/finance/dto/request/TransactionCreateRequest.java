package com.lifehub.finance.dto.request;

import com.lifehub.finance.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionCreateRequest(
        @NotNull Long accountId,
        @NotNull Long categoryId,
        @NotNull @Positive BigDecimal amount,
        @NotNull TransactionType type,
        @Size(max = 500) String memo,
        @NotNull LocalDate occurredAt
) {
}

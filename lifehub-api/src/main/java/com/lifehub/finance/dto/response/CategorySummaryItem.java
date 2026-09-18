package com.lifehub.finance.dto.response;

import com.lifehub.finance.entity.TransactionType;
import java.math.BigDecimal;

public record CategorySummaryItem(
        Long categoryId,
        String categoryName,
        TransactionType type,
        BigDecimal totalAmount
) {
}

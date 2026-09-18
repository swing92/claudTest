package com.lifehub.finance.repository;

import com.lifehub.finance.entity.TransactionType;
import java.math.BigDecimal;

public record CategoryAmountAggregate(Long categoryId, TransactionType type, BigDecimal totalAmount) {
}

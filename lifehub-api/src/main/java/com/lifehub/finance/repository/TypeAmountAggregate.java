package com.lifehub.finance.repository;

import com.lifehub.finance.entity.TransactionType;
import java.math.BigDecimal;

public record TypeAmountAggregate(TransactionType type, BigDecimal totalAmount) {
}

package com.lifehub.finance.dto.response;

import com.lifehub.finance.entity.Account;
import com.lifehub.finance.entity.AccountType;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String name,
        AccountType type,
        String currency,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrency(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}

package com.lifehub.finance.dto.request;

import com.lifehub.finance.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull AccountType type,
        @Size(min = 3, max = 3) String currency
) {
}

package com.lifehub.finance.dto.request;

import com.lifehub.finance.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull TransactionType type,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "colorHex must look like #RRGGBB") String colorHex,
        Boolean isDefault
) {
}

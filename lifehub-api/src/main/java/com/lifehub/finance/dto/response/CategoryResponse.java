package com.lifehub.finance.dto.response;

import com.lifehub.finance.entity.TransactionCategory;
import com.lifehub.finance.entity.TransactionType;
import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        TransactionType type,
        String colorHex,
        boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CategoryResponse from(TransactionCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getColorHex(),
                category.isDefault(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}

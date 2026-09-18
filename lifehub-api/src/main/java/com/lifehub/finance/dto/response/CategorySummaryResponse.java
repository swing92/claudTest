package com.lifehub.finance.dto.response;

import java.util.List;

public record CategorySummaryResponse(
        int year,
        int month,
        List<CategorySummaryItem> items
) {
}

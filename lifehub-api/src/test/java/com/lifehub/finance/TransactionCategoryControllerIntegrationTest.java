package com.lifehub.finance;

import com.lifehub.finance.dto.request.AccountCreateRequest;
import com.lifehub.finance.dto.request.CategoryCreateRequest;
import com.lifehub.finance.dto.request.CategoryUpdateRequest;
import com.lifehub.finance.entity.AccountType;
import com.lifehub.finance.entity.TransactionType;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionCategoryControllerIntegrationTest extends AbstractFinanceIntegrationTest {

    @Test
    void create_returnsCreatedCategory() throws Exception {
        var request = new CategoryCreateRequest("급여", TransactionType.INCOME, "#00AA00", true);

        mockMvc.perform(postJson("/api/finance/categories", request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("급여"))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.colorHex").value("#00AA00"))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void create_invalidColorHex_returns400() throws Exception {
        var request = new CategoryCreateRequest("식비", TransactionType.EXPENSE, "not-a-color", null);

        mockMvc.perform(postJson("/api/finance/categories", request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        var request = new CategoryCreateRequest("", TransactionType.EXPENSE, null, null);

        mockMvc.perform(postJson("/api/finance/categories", request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_filtersByType() throws Exception {
        createCategory("급여", TransactionType.INCOME);
        createCategory("식비", TransactionType.EXPENSE);
        createCategory("교통비", TransactionType.EXPENSE);

        mockMvc.perform(get("/api/finance/categories").param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/finance/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void get_nonExistentCategory_returns404() throws Exception {
        mockMvc.perform(get("/api/finance/categories/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_existingCategory_updatesFieldsButTypeStaysImmutable() throws Exception {
        long id = postAndGetId("/api/finance/categories",
                new CategoryCreateRequest("식비", TransactionType.EXPENSE, "#AA0000", false));

        var updateRequest = new CategoryUpdateRequest("식비/외식", "#CC0000", true);

        mockMvc.perform(putJson("/api/finance/categories/" + id, updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("식비/외식"))
                .andExpect(jsonPath("$.colorHex").value("#CC0000"))
                .andExpect(jsonPath("$.isDefault").value(true))
                // type is not part of CategoryUpdateRequest at all, so it must be unchanged.
                .andExpect(jsonPath("$.type").value("EXPENSE"));
    }

    @Test
    void update_nonExistentCategory_returns404() throws Exception {
        var updateRequest = new CategoryUpdateRequest("이름", null, null);

        mockMvc.perform(putJson("/api/finance/categories/999999", updateRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingCategory_returns204() throws Exception {
        long id = createCategory("삭제될카테고리", TransactionType.EXPENSE);

        mockMvc.perform(delete("/api/finance/categories/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/finance/categories/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_categoryReferencedByTransaction_returns409() throws Exception {
        long accountId = postAndGetId("/api/finance/accounts",
                new AccountCreateRequest("계좌", AccountType.CASH, null));
        long categoryId = createCategory("식비", TransactionType.EXPENSE);
        createTransaction(accountId, categoryId, TransactionType.EXPENSE, "5000", "2025-02-01");

        mockMvc.perform(delete("/api/finance/categories/" + categoryId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }
}

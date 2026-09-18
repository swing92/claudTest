package com.lifehub.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.lifehub.finance.dto.request.TransactionCreateRequest;
import com.lifehub.finance.dto.request.TransactionUpdateRequest;
import com.lifehub.finance.entity.AccountType;
import com.lifehub.finance.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerIntegrationTest extends AbstractFinanceIntegrationTest {

    @Test
    void create_returnsCreatedTransactionWithManualSource() throws Exception {
        long accountId = createAccount("계좌", AccountType.BANK);
        long categoryId = createCategory("급여", TransactionType.INCOME);
        var request = new TransactionCreateRequest(
                accountId, categoryId, new BigDecimal("1000000.00"), TransactionType.INCOME,
                "1월 급여", LocalDate.parse("2025-01-05"));

        mockMvc.perform(postJson("/api/finance/transactions", request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.amount").value(1000000.00))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void create_sourceFieldInRequestBodyIsIgnored_alwaysCreatesManual() throws Exception {
        // TransactionCreateRequest has no `source` field at all. Even if a client sends one anyway
        // (trying to fabricate a SYNCED row), Jackson silently drops the unknown JSON property and
        // the entity is still built via the MANUAL-only constructor. See docs/feature-specification.md
        // ("source는 API로 SYNCED를 만들 수 없다").
        long accountId = createAccount("계좌", AccountType.BANK);
        long categoryId = createCategory("식비", TransactionType.EXPENSE);
        String rawJsonWithForgedSource = """
                {
                  "accountId": %d,
                  "categoryId": %d,
                  "amount": 5000,
                  "type": "EXPENSE",
                  "occurredAt": "2025-01-10",
                  "source": "SYNCED",
                  "externalId": "forged-external-id-123"
                }
                """.formatted(accountId, categoryId);

        mockMvc.perform(post("/api/finance/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJsonWithForgedSource))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void create_typeMismatchWithCategoryType_returns400() throws Exception {
        long accountId = createAccount("계좌", AccountType.BANK);
        long expenseCategoryId = createCategory("식비", TransactionType.EXPENSE);
        var request = new TransactionCreateRequest(
                accountId, expenseCategoryId, new BigDecimal("1000"), TransactionType.INCOME,
                null, LocalDate.parse("2025-01-10"));

        mockMvc.perform(postJson("/api/finance/transactions", request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void create_nonExistentAccount_returns404() throws Exception {
        long categoryId = createCategory("식비", TransactionType.EXPENSE);
        var request = new TransactionCreateRequest(
                999999L, categoryId, new BigDecimal("1000"), TransactionType.EXPENSE,
                null, LocalDate.parse("2025-01-10"));

        mockMvc.perform(postJson("/api/finance/transactions", request))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_nonExistentCategory_returns404() throws Exception {
        long accountId = createAccount("계좌", AccountType.BANK);
        var request = new TransactionCreateRequest(
                accountId, 999999L, new BigDecimal("1000"), TransactionType.EXPENSE,
                null, LocalDate.parse("2025-01-10"));

        mockMvc.perform(postJson("/api/finance/transactions", request))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_nonPositiveAmount_returns400() throws Exception {
        long accountId = createAccount("계좌", AccountType.BANK);
        long categoryId = createCategory("식비", TransactionType.EXPENSE);
        var request = new TransactionCreateRequest(
                accountId, categoryId, new BigDecimal("0"), TransactionType.EXPENSE,
                null, LocalDate.parse("2025-01-10"));

        mockMvc.perform(postJson("/api/finance/transactions", request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void get_nonExistentTransaction_returns404() throws Exception {
        mockMvc.perform(get("/api/finance/transactions/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_existingTransaction_updatesFieldsAndRefreshesUpdatedAt() throws Exception {
        long accountId = createAccount("계좌", AccountType.BANK);
        long categoryId = createCategory("식비", TransactionType.EXPENSE);
        long txId = createTransaction(accountId, categoryId, TransactionType.EXPENSE, "5000", "2025-01-10");
        String created = mockMvc.perform(get("/api/finance/transactions/" + txId))
                .andReturn().getResponse().getContentAsString();
        String originalUpdatedAt = toJsonNode(created).get("updatedAt").asText();

        var updateRequest = new TransactionUpdateRequest(
                accountId, categoryId, new BigDecimal("7000"), TransactionType.EXPENSE,
                "수정됨", LocalDate.parse("2025-01-11"));

        MvcResult result = mockMvc.perform(putJson("/api/finance/transactions/" + txId, updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(7000))
                .andExpect(jsonPath("$.memo").value("수정됨"))
                .andReturn();

        String newUpdatedAt = toJsonNode(result.getResponse().getContentAsString()).get("updatedAt").asText();
        assertThat(newUpdatedAt).isNotEqualTo(originalUpdatedAt);
    }

    @Test
    void update_typeMismatchWithCategoryType_returns400() throws Exception {
        long accountId = createAccount("계좌", AccountType.BANK);
        long expenseCategoryId = createCategory("식비", TransactionType.EXPENSE);
        long txId = createTransaction(accountId, expenseCategoryId, TransactionType.EXPENSE, "5000", "2025-01-10");

        var updateRequest = new TransactionUpdateRequest(
                accountId, expenseCategoryId, new BigDecimal("5000"), TransactionType.INCOME,
                null, LocalDate.parse("2025-01-10"));

        mockMvc.perform(putJson("/api/finance/transactions/" + txId, updateRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_existingTransaction_returns204AndThenIsGone() throws Exception {
        long accountId = createAccount("계좌", AccountType.BANK);
        long categoryId = createCategory("식비", TransactionType.EXPENSE);
        long txId = createTransaction(accountId, categoryId, TransactionType.EXPENSE, "5000", "2025-01-10");

        mockMvc.perform(delete("/api/finance/transactions/" + txId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/finance/transactions/" + txId))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_filtersByAccountCategoryTypeAndDateRangeWithPaging() throws Exception {
        long accountA = createAccount("계좌A", AccountType.BANK);
        long accountB = createAccount("계좌B", AccountType.CASH);
        long incomeCategory = createCategory("급여", TransactionType.INCOME);
        long expenseCategory = createCategory("식비", TransactionType.EXPENSE);

        createTransaction(accountA, incomeCategory, TransactionType.INCOME, "1000", "2025-04-01");
        createTransaction(accountA, expenseCategory, TransactionType.EXPENSE, "2000", "2025-04-05");
        createTransaction(accountB, expenseCategory, TransactionType.EXPENSE, "3000", "2025-04-10");
        createTransaction(accountA, expenseCategory, TransactionType.EXPENSE, "4000", "2025-05-01");

        // accountId filter
        mockMvc.perform(get("/api/finance/transactions").param("accountId", String.valueOf(accountA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        // type filter
        mockMvc.perform(get("/api/finance/transactions").param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        // date range filter (April only)
        mockMvc.perform(get("/api/finance/transactions")
                        .param("from", "2025-04-01").param("to", "2025-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        // combined: accountA + EXPENSE + April
        mockMvc.perform(get("/api/finance/transactions")
                        .param("accountId", String.valueOf(accountA))
                        .param("type", "EXPENSE")
                        .param("from", "2025-04-01").param("to", "2025-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // paging
        mockMvc.perform(get("/api/finance/transactions").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void monthlySummary_computesIncomeExpenseAndNetForGivenMonth() throws Exception {
        long accountId = createAccount("계좌", AccountType.BANK);
        long incomeCategory = createCategory("급여", TransactionType.INCOME);
        long expenseCategory = createCategory("식비", TransactionType.EXPENSE);

        createTransaction(accountId, incomeCategory, TransactionType.INCOME, "3000000", "2025-06-05");
        createTransaction(accountId, expenseCategory, TransactionType.EXPENSE, "15000", "2025-06-10");
        createTransaction(accountId, expenseCategory, TransactionType.EXPENSE, "25000", "2025-06-20");
        // outside the queried month — must not be counted
        createTransaction(accountId, expenseCategory, TransactionType.EXPENSE, "999999", "2025-07-01");

        mockMvc.perform(get("/api/finance/transactions/summary/monthly")
                        .param("year", "2025").param("month", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2025))
                .andExpect(jsonPath("$.month").value(6))
                .andExpect(jsonPath("$.totalIncome").value(3000000.00))
                .andExpect(jsonPath("$.totalExpense").value(40000.00))
                .andExpect(jsonPath("$.netAmount").value(2960000.00));
    }

    @Test
    void monthlySummary_monthWithNoTransactions_returnsZeroes() throws Exception {
        mockMvc.perform(get("/api/finance/transactions/summary/monthly")
                        .param("year", "2025").param("month", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.netAmount").value(0));
    }

    @Test
    void categorySummary_groupsAmountsByCategoryForGivenMonth() throws Exception {
        long accountId = createAccount("계좌", AccountType.BANK);
        long foodCategory = createCategory("식비", TransactionType.EXPENSE);
        long transportCategory = createCategory("교통비", TransactionType.EXPENSE);

        createTransaction(accountId, foodCategory, TransactionType.EXPENSE, "10000", "2025-09-01");
        createTransaction(accountId, foodCategory, TransactionType.EXPENSE, "20000", "2025-09-02");
        createTransaction(accountId, transportCategory, TransactionType.EXPENSE, "5000", "2025-09-03");

        MvcResult result = mockMvc.perform(get("/api/finance/transactions/summary/by-category")
                        .param("year", "2025").param("month", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andReturn();

        JsonNode items = toJsonNode(result.getResponse().getContentAsString()).get("items");
        Map<Long, BigDecimal> totalsByCategoryId = new HashMap<>();
        items.forEach(item -> totalsByCategoryId.put(
                item.get("categoryId").asLong(), new BigDecimal(item.get("totalAmount").asText())));

        assertThat(totalsByCategoryId.get(foodCategory)).isEqualByComparingTo("30000");
        assertThat(totalsByCategoryId.get(transportCategory)).isEqualByComparingTo("5000");
    }
}

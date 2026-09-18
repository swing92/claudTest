package com.lifehub.finance;

import com.lifehub.finance.dto.request.AccountCreateRequest;
import com.lifehub.finance.dto.request.AccountUpdateRequest;
import com.lifehub.finance.entity.AccountType;
import com.lifehub.finance.entity.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountControllerIntegrationTest extends AbstractFinanceIntegrationTest {

    @Test
    void create_returnsCreatedAccountWithDefaultCurrency() throws Exception {
        var request = new AccountCreateRequest("주거래통장", AccountType.BANK, null);

        mockMvc.perform(postJson("/api/finance/accounts", request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("주거래통장"))
                .andExpect(jsonPath("$.type").value("BANK"))
                .andExpect(jsonPath("$.currency").value("KRW"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        var request = new AccountCreateRequest("   ", AccountType.CASH, "KRW");

        mockMvc.perform(postJson("/api/finance/accounts", request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void list_returnsAllCreatedAccounts() throws Exception {
        createAccount("계좌1", AccountType.CASH);
        createAccount("계좌2", AccountType.BANK);

        mockMvc.perform(get("/api/finance/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void get_existingAccount_returnsIt() throws Exception {
        long id = createAccount("계좌", AccountType.CARD);

        mockMvc.perform(get("/api/finance/accounts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.type").value("CARD"));
    }

    @Test
    void get_nonExistentAccount_returns404() throws Exception {
        mockMvc.perform(get("/api/finance/accounts/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void update_existingAccount_updatesFieldsAndRefreshesUpdatedAt() throws Exception {
        long id = createAccount("이전이름", AccountType.CASH);
        String created = mockMvc.perform(get("/api/finance/accounts/" + id))
                .andReturn().getResponse().getContentAsString();
        String originalUpdatedAt = toJsonNode(created).get("updatedAt").asText();

        var updateRequest = new AccountUpdateRequest("새이름", AccountType.BANK, "USD");

        MvcResult result = mockMvc.perform(putJson("/api/finance/accounts/" + id, updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("새이름"))
                .andExpect(jsonPath("$.type").value("BANK"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andReturn();

        String newUpdatedAt = toJsonNode(result.getResponse().getContentAsString()).get("updatedAt").asText();
        assertThat(newUpdatedAt).isNotEqualTo(originalUpdatedAt);
    }

    @Test
    void update_nonExistentAccount_returns404() throws Exception {
        var updateRequest = new AccountUpdateRequest("이름", AccountType.CASH, "KRW");

        mockMvc.perform(putJson("/api/finance/accounts/999999", updateRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingAccount_returns204AndThenIsGone() throws Exception {
        long id = createAccount("삭제될계좌", AccountType.CASH);

        mockMvc.perform(delete("/api/finance/accounts/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/finance/accounts/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_accountReferencedByTransaction_returns409() throws Exception {
        long accountId = createAccount("참조계좌", AccountType.BANK);
        long categoryId = createCategory("급여", TransactionType.INCOME);
        createTransaction(accountId, categoryId, TransactionType.INCOME, "100.00", "2025-01-10");

        mockMvc.perform(delete("/api/finance/accounts/" + accountId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }
}

package com.lifehub.finance;

import com.lifehub.AbstractIntegrationTest;
import com.lifehub.finance.dto.request.AccountCreateRequest;
import com.lifehub.finance.dto.request.CategoryCreateRequest;
import com.lifehub.finance.dto.request.TransactionCreateRequest;
import com.lifehub.finance.entity.AccountType;
import com.lifehub.finance.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

abstract class AbstractFinanceIntegrationTest extends AbstractIntegrationTest {

    protected long createAccount(String name, AccountType type) throws Exception {
        return postAndGetId("/api/finance/accounts", new AccountCreateRequest(name, type, null));
    }

    protected long createCategory(String name, TransactionType type) throws Exception {
        return postAndGetId("/api/finance/categories", new CategoryCreateRequest(name, type, null, null));
    }

    protected long createTransaction(long accountId, long categoryId, TransactionType type,
                                      String amount, String occurredAt) throws Exception {
        var request = new TransactionCreateRequest(
                accountId, categoryId, new BigDecimal(amount), type, null, LocalDate.parse(occurredAt));
        return postAndGetId("/api/finance/transactions", request);
    }
}

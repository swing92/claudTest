package com.lifehub.finance.service;

import com.lifehub.common.exception.ConflictException;
import com.lifehub.common.exception.ResourceNotFoundException;
import com.lifehub.finance.dto.request.AccountCreateRequest;
import com.lifehub.finance.dto.request.AccountUpdateRequest;
import com.lifehub.finance.dto.response.AccountResponse;
import com.lifehub.finance.entity.Account;
import com.lifehub.finance.repository.AccountRepository;
import com.lifehub.finance.repository.TransactionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private static final String DEFAULT_CURRENCY = "KRW";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public AccountResponse create(Long userId, AccountCreateRequest request) {
        String currency = request.currency() == null || request.currency().isBlank()
                ? DEFAULT_CURRENCY
                : request.currency();
        Account account = new Account(userId, request.name(), request.type(), currency);
        return AccountResponse.from(accountRepository.save(account));
    }

    public List<AccountResponse> list(Long userId) {
        return accountRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    public AccountResponse get(Long userId, Long accountId) {
        return AccountResponse.from(findOwnedOrThrow(userId, accountId));
    }

    @Transactional
    public AccountResponse update(Long userId, Long accountId, AccountUpdateRequest request) {
        Account account = findOwnedOrThrow(userId, accountId);
        String currency = request.currency() == null || request.currency().isBlank()
                ? DEFAULT_CURRENCY
                : request.currency();
        account.update(request.name(), request.type(), currency);
        // Force the pending UPDATE (and its @PreUpdate-driven updatedAt refresh) to run now,
        // so the response reflects it instead of the pre-flush in-memory value.
        accountRepository.flush();
        return AccountResponse.from(account);
    }

    @Transactional
    public void delete(Long userId, Long accountId) {
        Account account = findOwnedOrThrow(userId, accountId);
        if (transactionRepository.existsByAccountId(accountId)) {
            throw new ConflictException("이 계좌를 참조하는 거래 내역이 있어 삭제할 수 없습니다.");
        }
        accountRepository.delete(account);
    }

    private Account findOwnedOrThrow(Long userId, Long accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
    }
}

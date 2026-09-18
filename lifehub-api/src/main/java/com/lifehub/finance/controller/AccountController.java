package com.lifehub.finance.controller;

import com.lifehub.common.security.CurrentUserId;
import com.lifehub.finance.dto.request.AccountCreateRequest;
import com.lifehub.finance.dto.request.AccountUpdateRequest;
import com.lifehub.finance.dto.response.AccountResponse;
import com.lifehub.finance.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@CurrentUserId Long userId,
                                                    @Valid @RequestBody AccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(userId, request));
    }

    @GetMapping
    public List<AccountResponse> list(@CurrentUserId Long userId) {
        return accountService.list(userId);
    }

    @GetMapping("/{accountId}")
    public AccountResponse get(@CurrentUserId Long userId, @PathVariable Long accountId) {
        return accountService.get(userId, accountId);
    }

    @PutMapping("/{accountId}")
    public AccountResponse update(@CurrentUserId Long userId, @PathVariable Long accountId,
                                   @Valid @RequestBody AccountUpdateRequest request) {
        return accountService.update(userId, accountId, request);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long accountId) {
        accountService.delete(userId, accountId);
        return ResponseEntity.noContent().build();
    }
}

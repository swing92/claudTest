package com.lifehub.finance.controller;

import com.lifehub.common.security.CurrentUserId;
import com.lifehub.finance.dto.request.TransactionCreateRequest;
import com.lifehub.finance.dto.request.TransactionUpdateRequest;
import com.lifehub.finance.dto.response.CategorySummaryResponse;
import com.lifehub.finance.dto.response.MonthlySummaryResponse;
import com.lifehub.finance.dto.response.TransactionResponse;
import com.lifehub.finance.entity.TransactionType;
import com.lifehub.finance.service.TransactionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@CurrentUserId Long userId,
                                                        @Valid @RequestBody TransactionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(userId, request));
    }

    @GetMapping
    public Page<TransactionResponse> search(
            @CurrentUserId Long userId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.search(userId, accountId, categoryId, type, from, to, pageable);
    }

    @GetMapping("/summary/monthly")
    public MonthlySummaryResponse monthlySummary(@CurrentUserId Long userId,
                                                   @RequestParam int year,
                                                   @RequestParam int month) {
        return transactionService.monthlySummary(userId, year, month);
    }

    @GetMapping("/summary/by-category")
    public CategorySummaryResponse categorySummary(@CurrentUserId Long userId,
                                                     @RequestParam int year,
                                                     @RequestParam int month) {
        return transactionService.categorySummary(userId, year, month);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse get(@CurrentUserId Long userId, @PathVariable Long transactionId) {
        return transactionService.get(userId, transactionId);
    }

    @PutMapping("/{transactionId}")
    public TransactionResponse update(@CurrentUserId Long userId, @PathVariable Long transactionId,
                                       @Valid @RequestBody TransactionUpdateRequest request) {
        return transactionService.update(userId, transactionId, request);
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long transactionId) {
        transactionService.delete(userId, transactionId);
        return ResponseEntity.noContent().build();
    }
}

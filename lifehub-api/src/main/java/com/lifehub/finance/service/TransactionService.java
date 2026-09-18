package com.lifehub.finance.service;

import com.lifehub.common.exception.ResourceNotFoundException;
import com.lifehub.finance.dto.request.TransactionCreateRequest;
import com.lifehub.finance.dto.request.TransactionUpdateRequest;
import com.lifehub.finance.dto.response.CategorySummaryItem;
import com.lifehub.finance.dto.response.CategorySummaryResponse;
import com.lifehub.finance.dto.response.MonthlySummaryResponse;
import com.lifehub.finance.dto.response.TransactionResponse;
import com.lifehub.finance.entity.Transaction;
import com.lifehub.finance.entity.TransactionCategory;
import com.lifehub.finance.entity.TransactionType;
import com.lifehub.finance.repository.AccountRepository;
import com.lifehub.finance.repository.CategoryAmountAggregate;
import com.lifehub.finance.repository.TransactionCategoryRepository;
import com.lifehub.finance.repository.TransactionRepository;
import com.lifehub.finance.repository.TransactionSpecifications;
import com.lifehub.finance.repository.TypeAmountAggregate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository,
                               AccountRepository accountRepository,
                               TransactionCategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public TransactionResponse create(Long userId, TransactionCreateRequest request) {
        requireOwnedAccount(userId, request.accountId());
        requireCategoryMatchingType(userId, request.categoryId(), request.type());

        Transaction transaction = new Transaction(
                userId, request.accountId(), request.categoryId(), request.amount(),
                request.type(), request.memo(), request.occurredAt());
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    public Page<TransactionResponse> search(Long userId, Long accountId, Long categoryId,
                                             TransactionType type, LocalDate from, LocalDate to,
                                             Pageable pageable) {
        Specification<Transaction> spec = Specification
                .where(TransactionSpecifications.userIdEquals(userId))
                .and(TransactionSpecifications.accountIdEquals(accountId))
                .and(TransactionSpecifications.categoryIdEquals(categoryId))
                .and(TransactionSpecifications.typeEquals(type))
                .and(TransactionSpecifications.occurredAtFrom(from))
                .and(TransactionSpecifications.occurredAtTo(to));
        return transactionRepository.findAll(spec, pageable).map(TransactionResponse::from);
    }

    public TransactionResponse get(Long userId, Long transactionId) {
        return TransactionResponse.from(findOwnedOrThrow(userId, transactionId));
    }

    @Transactional
    public TransactionResponse update(Long userId, Long transactionId, TransactionUpdateRequest request) {
        Transaction transaction = findOwnedOrThrow(userId, transactionId);
        requireOwnedAccount(userId, request.accountId());
        requireCategoryMatchingType(userId, request.categoryId(), request.type());

        transaction.update(request.accountId(), request.categoryId(), request.amount(),
                request.type(), request.memo(), request.occurredAt());
        // Force the pending UPDATE (and its @PreUpdate-driven updatedAt refresh) to run now,
        // so the response reflects it instead of the pre-flush in-memory value.
        transactionRepository.flush();
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(Long userId, Long transactionId) {
        Transaction transaction = findOwnedOrThrow(userId, transactionId);
        transactionRepository.delete(transaction);
    }

    public MonthlySummaryResponse monthlySummary(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        List<TypeAmountAggregate> aggregates = transactionRepository.sumAmountByTypeBetween(
                userId, yearMonth.atDay(1), yearMonth.atEndOfMonth());

        BigDecimal income = amountFor(aggregates, TransactionType.INCOME);
        BigDecimal expense = amountFor(aggregates, TransactionType.EXPENSE);
        return new MonthlySummaryResponse(year, month, income, expense, income.subtract(expense));
    }

    public CategorySummaryResponse categorySummary(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        List<CategoryAmountAggregate> aggregates = transactionRepository.sumAmountByCategoryBetween(
                userId, yearMonth.atDay(1), yearMonth.atEndOfMonth());

        List<Long> categoryIds = aggregates.stream().map(CategoryAmountAggregate::categoryId).distinct().toList();
        Map<Long, String> categoryNames = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(TransactionCategory::getId, TransactionCategory::getName));

        List<CategorySummaryItem> items = aggregates.stream()
                .map(a -> new CategorySummaryItem(
                        a.categoryId(),
                        categoryNames.getOrDefault(a.categoryId(), "(deleted category)"),
                        a.type(),
                        a.totalAmount()))
                .toList();
        return new CategorySummaryResponse(year, month, items);
    }

    private static BigDecimal amountFor(List<TypeAmountAggregate> aggregates, TransactionType type) {
        return aggregates.stream()
                .filter(a -> a.type() == type)
                .map(TypeAmountAggregate::totalAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private Transaction findOwnedOrThrow(Long userId, Long transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
    }

    private void requireOwnedAccount(Long userId, Long accountId) {
        if (accountRepository.findByIdAndUserId(accountId, userId).isEmpty()) {
            throw new ResourceNotFoundException("Account not found: " + accountId);
        }
    }

    private void requireCategoryMatchingType(Long userId, Long categoryId, TransactionType type) {
        TransactionCategory category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        if (category.getType() != type) {
            throw new IllegalArgumentException(
                    "transaction type (%s) does not match category type (%s)".formatted(type, category.getType()));
        }
    }
}

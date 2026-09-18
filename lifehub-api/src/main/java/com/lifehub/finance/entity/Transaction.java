package com.lifehub.finance.entity;

import com.lifehub.common.entity.BaseUserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "transaction")
public class Transaction extends BaseUserOwnedEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(length = 500)
    private String memo;

    @Column(name = "occurred_at", nullable = false)
    private LocalDate occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionSource source;

    @Column(name = "external_id")
    private String externalId;

    /**
     * v1 only ever creates MANUAL transactions with no external_id; SYNCED rows (with a real
     * external_id from a bank/card feed) will get their own creation path once auto-sync exists.
     */
    public Transaction(Long userId, Long accountId, Long categoryId, BigDecimal amount,
                        TransactionType type, String memo, LocalDate occurredAt) {
        super(userId);
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.type = type;
        this.memo = memo;
        this.occurredAt = occurredAt;
        this.source = TransactionSource.MANUAL;
    }

    public void update(Long accountId, Long categoryId, BigDecimal amount,
                        TransactionType type, String memo, LocalDate occurredAt) {
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.type = type;
        this.memo = memo;
        this.occurredAt = occurredAt;
    }
}

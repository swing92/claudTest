package com.lifehub.finance.entity;

import com.lifehub.common.entity.BaseUserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "transaction_category")
public class TransactionCategory extends BaseUserOwnedEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    public TransactionCategory(Long userId, String name, TransactionType type, String colorHex, boolean isDefault) {
        super(userId);
        this.name = name;
        this.type = type;
        this.colorHex = colorHex;
        this.isDefault = isDefault;
    }

    /** Type is intentionally not editable: existing transactions denormalize this category's type. */
    public void update(String name, String colorHex, boolean isDefault) {
        this.name = name;
        this.colorHex = colorHex;
        this.isDefault = isDefault;
    }
}

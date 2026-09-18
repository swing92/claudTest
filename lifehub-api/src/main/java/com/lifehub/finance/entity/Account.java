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
@Table(name = "account")
public class Account extends BaseUserOwnedEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Column(nullable = false, length = 3)
    private String currency;

    public Account(Long userId, String name, AccountType type, String currency) {
        super(userId);
        this.name = name;
        this.type = type;
        this.currency = currency;
    }

    public void update(String name, AccountType type, String currency) {
        this.name = name;
        this.type = type;
        this.currency = currency;
    }
}

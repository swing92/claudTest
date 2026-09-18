package com.lifehub.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * userId is stored as a plain column (not a JPA relation to the User entity)
 * so domain modules stay decoupled from the user package and from each other.
 */
@Getter
@MappedSuperclass
public abstract class BaseUserOwnedEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    protected BaseUserOwnedEntity() {
    }

    protected BaseUserOwnedEntity(Long userId) {
        this.userId = userId;
    }
}

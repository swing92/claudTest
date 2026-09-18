package com.lifehub.jobapplications.entity;

import com.lifehub.common.entity.BaseUserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "company")
public class Company extends BaseUserOwnedEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String industry;

    @Column(length = 500)
    private String url;

    @Column(length = 2000)
    private String notes;

    public Company(Long userId, String name, String industry, String url, String notes) {
        super(userId);
        this.name = name;
        this.industry = industry;
        this.url = url;
        this.notes = notes;
    }

    public void update(String name, String industry, String url, String notes) {
        this.name = name;
        this.industry = industry;
        this.url = url;
        this.notes = notes;
    }
}

package com.lifehub.jobapplications.entity;

import com.lifehub.common.entity.BaseUserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "job_application")
public class JobApplication extends BaseUserOwnedEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "position_title", nullable = false, length = 200)
    private String positionTitle;

    @Column(name = "apply_url", length = 500)
    private String applyUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobApplicationStatus status;

    @Column(name = "applied_at")
    private LocalDate appliedAt;

    @Column(length = 2000)
    private String notes;

    public JobApplication(Long userId, Long companyId, String positionTitle, String applyUrl,
                           JobApplicationStatus status, LocalDate appliedAt, String notes) {
        super(userId);
        this.companyId = companyId;
        this.positionTitle = positionTitle;
        this.applyUrl = applyUrl;
        this.status = status;
        this.appliedAt = appliedAt;
        this.notes = notes;
    }

    public void update(Long companyId, String positionTitle, String applyUrl,
                        JobApplicationStatus status, LocalDate appliedAt, String notes) {
        this.companyId = companyId;
        this.positionTitle = positionTitle;
        this.applyUrl = applyUrl;
        this.status = status;
        this.appliedAt = appliedAt;
        this.notes = notes;
    }
}

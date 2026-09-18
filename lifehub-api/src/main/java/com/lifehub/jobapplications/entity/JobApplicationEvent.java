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

/**
 * userId here is intentionally denormalized from the parent JobApplication's userId (every table
 * carries user_id, per project convention) — see docs/table-definition.md. Postgres can't check
 * that the two stay equal with a CHECK constraint (it can't reference another table), so
 * JobApplicationEventService verifies it explicitly before ever constructing this entity.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "job_application_event")
public class JobApplicationEvent extends BaseUserOwnedEntity {

    @Column(name = "job_application_id", nullable = false)
    private Long jobApplicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private JobApplicationEventType eventType;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JobApplicationEventResult result;

    @Column(length = 2000)
    private String memo;

    public JobApplicationEvent(Long userId, Long jobApplicationId, JobApplicationEventType eventType,
                                LocalDate eventDate, JobApplicationEventResult result, String memo) {
        super(userId);
        this.jobApplicationId = jobApplicationId;
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.result = result;
        this.memo = memo;
    }

    public void update(JobApplicationEventType eventType, LocalDate eventDate,
                        JobApplicationEventResult result, String memo) {
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.result = result;
        this.memo = memo;
    }
}

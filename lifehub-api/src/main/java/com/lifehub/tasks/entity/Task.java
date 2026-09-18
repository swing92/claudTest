package com.lifehub.tasks.entity;

import com.lifehub.common.entity.BaseUserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "task")
public class Task extends BaseUserOwnedEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskType type;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "is_all_day", nullable = false)
    private boolean isAllDay;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskPriority priority;

    public Task(Long userId, String title, String description, TaskType type,
                LocalDateTime dueAt, LocalDateTime startAt, LocalDateTime endAt,
                boolean isAllDay, TaskPriority priority) {
        super(userId);
        this.title = title;
        this.description = description;
        this.type = type;
        this.dueAt = dueAt;
        this.startAt = startAt;
        this.endAt = endAt;
        this.isAllDay = isAllDay;
        this.priority = priority;
        this.isCompleted = false;
    }

    /** Completion state is intentionally not editable here: it only changes via {@link #complete()}/{@link #reopen()}. */
    public void update(String title, String description, TaskType type,
                        LocalDateTime dueAt, LocalDateTime startAt, LocalDateTime endAt,
                        boolean isAllDay, TaskPriority priority) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.dueAt = dueAt;
        this.startAt = startAt;
        this.endAt = endAt;
        this.isAllDay = isAllDay;
        this.priority = priority;
    }

    public void complete() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
    }

    public void reopen() {
        this.isCompleted = false;
        this.completedAt = null;
    }
}

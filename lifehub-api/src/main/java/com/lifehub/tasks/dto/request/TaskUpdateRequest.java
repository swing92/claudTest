package com.lifehub.tasks.dto.request;

import com.lifehub.tasks.entity.TaskPriority;
import com.lifehub.tasks.entity.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Completion state (isCompleted/completedAt) is not here: it changes only via the complete/incomplete actions. */
public record TaskUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull TaskType type,
        LocalDateTime dueAt,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Boolean isAllDay,
        TaskPriority priority
) {
}

package com.lifehub.tasks.dto.response;

import com.lifehub.tasks.entity.Task;
import com.lifehub.tasks.entity.TaskPriority;
import com.lifehub.tasks.entity.TaskType;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskType type,
        LocalDateTime dueAt,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean isAllDay,
        boolean isCompleted,
        LocalDateTime completedAt,
        TaskPriority priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getType(),
                task.getDueAt(),
                task.getStartAt(),
                task.getEndAt(),
                task.isAllDay(),
                task.isCompleted(),
                task.getCompletedAt(),
                task.getPriority(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}

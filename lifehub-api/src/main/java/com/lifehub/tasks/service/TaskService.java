package com.lifehub.tasks.service;

import com.lifehub.common.exception.ResourceNotFoundException;
import com.lifehub.tasks.dto.request.TaskCreateRequest;
import com.lifehub.tasks.dto.request.TaskUpdateRequest;
import com.lifehub.tasks.dto.response.TaskResponse;
import com.lifehub.tasks.entity.Task;
import com.lifehub.tasks.entity.TaskPriority;
import com.lifehub.tasks.entity.TaskType;
import com.lifehub.tasks.repository.TaskRepository;
import com.lifehub.tasks.repository.TaskSpecifications;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private static final TaskPriority DEFAULT_PRIORITY = TaskPriority.MEDIUM;

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TaskResponse create(Long userId, TaskCreateRequest request) {
        requireValidRange(request.startAt(), request.endAt());
        Task task = new Task(
                userId, request.title(), request.description(), request.type(),
                request.dueAt(), request.startAt(), request.endAt(),
                Boolean.TRUE.equals(request.isAllDay()),
                request.priority() == null ? DEFAULT_PRIORITY : request.priority());
        return TaskResponse.from(taskRepository.save(task));
    }

    public Page<TaskResponse> search(Long userId, TaskType type, Boolean isCompleted,
                                      LocalDateTime dueFrom, LocalDateTime dueTo, Pageable pageable) {
        Specification<Task> spec = Specification
                .where(TaskSpecifications.userIdEquals(userId))
                .and(TaskSpecifications.typeEquals(type))
                .and(TaskSpecifications.isCompletedEquals(isCompleted))
                .and(TaskSpecifications.dueAtFrom(dueFrom))
                .and(TaskSpecifications.dueAtTo(dueTo));
        return taskRepository.findAll(spec, pageable).map(TaskResponse::from);
    }

    public TaskResponse get(Long userId, Long taskId) {
        return TaskResponse.from(findOwnedOrThrow(userId, taskId));
    }

    @Transactional
    public TaskResponse update(Long userId, Long taskId, TaskUpdateRequest request) {
        requireValidRange(request.startAt(), request.endAt());
        Task task = findOwnedOrThrow(userId, taskId);
        task.update(request.title(), request.description(), request.type(),
                request.dueAt(), request.startAt(), request.endAt(),
                Boolean.TRUE.equals(request.isAllDay()),
                request.priority() == null ? DEFAULT_PRIORITY : request.priority());
        // See TROUBLESHOOTING.md ("updatedAt이 flush 이전 값"): flush now so the response reflects
        // the refreshed @LastModifiedDate instead of the pre-flush in-memory value.
        taskRepository.flush();
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse complete(Long userId, Long taskId) {
        Task task = findOwnedOrThrow(userId, taskId);
        task.complete();
        taskRepository.flush();
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse reopen(Long userId, Long taskId) {
        Task task = findOwnedOrThrow(userId, taskId);
        task.reopen();
        taskRepository.flush();
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long userId, Long taskId) {
        Task task = findOwnedOrThrow(userId, taskId);
        taskRepository.delete(task);
    }

    private static void requireValidRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("endAt must not be before startAt");
        }
    }

    private Task findOwnedOrThrow(Long userId, Long taskId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
    }
}

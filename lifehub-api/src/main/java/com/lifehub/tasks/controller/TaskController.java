package com.lifehub.tasks.controller;

import com.lifehub.common.security.CurrentUserId;
import com.lifehub.tasks.dto.request.TaskCreateRequest;
import com.lifehub.tasks.dto.request.TaskUpdateRequest;
import com.lifehub.tasks.dto.response.TaskResponse;
import com.lifehub.tasks.entity.TaskType;
import com.lifehub.tasks.service.TaskService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@CurrentUserId Long userId,
                                                @Valid @RequestBody TaskCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(userId, request));
    }

    @GetMapping
    public Page<TaskResponse> search(
            @CurrentUserId Long userId,
            @RequestParam(required = false) TaskType type,
            @RequestParam(required = false) Boolean isCompleted,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueTo,
            @PageableDefault(size = 20, sort = "dueAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return taskService.search(userId, type, isCompleted, dueFrom, dueTo, pageable);
    }

    @GetMapping("/{taskId}")
    public TaskResponse get(@CurrentUserId Long userId, @PathVariable Long taskId) {
        return taskService.get(userId, taskId);
    }

    @PutMapping("/{taskId}")
    public TaskResponse update(@CurrentUserId Long userId, @PathVariable Long taskId,
                                @Valid @RequestBody TaskUpdateRequest request) {
        return taskService.update(userId, taskId, request);
    }

    @PatchMapping("/{taskId}/complete")
    public TaskResponse complete(@CurrentUserId Long userId, @PathVariable Long taskId) {
        return taskService.complete(userId, taskId);
    }

    @PatchMapping("/{taskId}/incomplete")
    public TaskResponse incomplete(@CurrentUserId Long userId, @PathVariable Long taskId) {
        return taskService.reopen(userId, taskId);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long taskId) {
        taskService.delete(userId, taskId);
        return ResponseEntity.noContent().build();
    }
}

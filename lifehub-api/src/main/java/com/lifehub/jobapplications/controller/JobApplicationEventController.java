package com.lifehub.jobapplications.controller;

import com.lifehub.common.security.CurrentUserId;
import com.lifehub.jobapplications.dto.request.JobApplicationEventCreateRequest;
import com.lifehub.jobapplications.dto.request.JobApplicationEventUpdateRequest;
import com.lifehub.jobapplications.dto.response.JobApplicationEventResponse;
import com.lifehub.jobapplications.service.JobApplicationEventService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-applications/{jobApplicationId}/events")
public class JobApplicationEventController {

    private final JobApplicationEventService jobApplicationEventService;

    public JobApplicationEventController(JobApplicationEventService jobApplicationEventService) {
        this.jobApplicationEventService = jobApplicationEventService;
    }

    @PostMapping
    public ResponseEntity<JobApplicationEventResponse> create(
            @CurrentUserId Long userId, @PathVariable Long jobApplicationId,
            @Valid @RequestBody JobApplicationEventCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobApplicationEventService.create(userId, jobApplicationId, request));
    }

    @GetMapping
    public List<JobApplicationEventResponse> list(@CurrentUserId Long userId, @PathVariable Long jobApplicationId) {
        return jobApplicationEventService.list(userId, jobApplicationId);
    }

    @GetMapping("/{eventId}")
    public JobApplicationEventResponse get(@CurrentUserId Long userId, @PathVariable Long jobApplicationId,
                                            @PathVariable Long eventId) {
        return jobApplicationEventService.get(userId, jobApplicationId, eventId);
    }

    @PutMapping("/{eventId}")
    public JobApplicationEventResponse update(@CurrentUserId Long userId, @PathVariable Long jobApplicationId,
                                               @PathVariable Long eventId,
                                               @Valid @RequestBody JobApplicationEventUpdateRequest request) {
        return jobApplicationEventService.update(userId, jobApplicationId, eventId, request);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long jobApplicationId,
                                        @PathVariable Long eventId) {
        jobApplicationEventService.delete(userId, jobApplicationId, eventId);
        return ResponseEntity.noContent().build();
    }
}

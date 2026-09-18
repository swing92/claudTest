package com.lifehub.jobapplications.controller;

import com.lifehub.common.security.CurrentUserId;
import com.lifehub.jobapplications.dto.request.JobApplicationCreateRequest;
import com.lifehub.jobapplications.dto.request.JobApplicationUpdateRequest;
import com.lifehub.jobapplications.dto.response.JobApplicationResponse;
import com.lifehub.jobapplications.entity.JobApplicationStatus;
import com.lifehub.jobapplications.service.JobApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    public JobApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    @PostMapping
    public ResponseEntity<JobApplicationResponse> create(@CurrentUserId Long userId,
                                                           @Valid @RequestBody JobApplicationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobApplicationService.create(userId, request));
    }

    @GetMapping
    public Page<JobApplicationResponse> search(
            @CurrentUserId Long userId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) JobApplicationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return jobApplicationService.search(userId, companyId, status, pageable);
    }

    @GetMapping("/{jobApplicationId}")
    public JobApplicationResponse get(@CurrentUserId Long userId, @PathVariable Long jobApplicationId) {
        return jobApplicationService.get(userId, jobApplicationId);
    }

    @PutMapping("/{jobApplicationId}")
    public JobApplicationResponse update(@CurrentUserId Long userId, @PathVariable Long jobApplicationId,
                                          @Valid @RequestBody JobApplicationUpdateRequest request) {
        return jobApplicationService.update(userId, jobApplicationId, request);
    }

    @DeleteMapping("/{jobApplicationId}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long jobApplicationId) {
        jobApplicationService.delete(userId, jobApplicationId);
        return ResponseEntity.noContent().build();
    }
}

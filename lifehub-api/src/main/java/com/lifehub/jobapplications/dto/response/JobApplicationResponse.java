package com.lifehub.jobapplications.dto.response;

import com.lifehub.jobapplications.entity.JobApplication;
import com.lifehub.jobapplications.entity.JobApplicationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record JobApplicationResponse(
        Long id,
        Long companyId,
        String positionTitle,
        String applyUrl,
        JobApplicationStatus status,
        LocalDate appliedAt,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static JobApplicationResponse from(JobApplication jobApplication) {
        return new JobApplicationResponse(
                jobApplication.getId(),
                jobApplication.getCompanyId(),
                jobApplication.getPositionTitle(),
                jobApplication.getApplyUrl(),
                jobApplication.getStatus(),
                jobApplication.getAppliedAt(),
                jobApplication.getNotes(),
                jobApplication.getCreatedAt(),
                jobApplication.getUpdatedAt());
    }
}

package com.lifehub.jobapplications.dto.request;

import com.lifehub.jobapplications.entity.JobApplicationEventResult;
import com.lifehub.jobapplications.entity.JobApplicationEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** jobApplicationId is not here: it comes from the URL path (/api/job-applications/{jobApplicationId}/events). */
public record JobApplicationEventCreateRequest(
        @NotNull JobApplicationEventType eventType,
        @NotNull LocalDate eventDate,
        JobApplicationEventResult result,
        @Size(max = 2000) String memo
) {
}

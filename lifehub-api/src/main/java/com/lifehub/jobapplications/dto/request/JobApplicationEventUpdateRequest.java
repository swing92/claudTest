package com.lifehub.jobapplications.dto.request;

import com.lifehub.jobapplications.entity.JobApplicationEventResult;
import com.lifehub.jobapplications.entity.JobApplicationEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record JobApplicationEventUpdateRequest(
        @NotNull JobApplicationEventType eventType,
        @NotNull LocalDate eventDate,
        @NotNull JobApplicationEventResult result,
        @Size(max = 2000) String memo
) {
}

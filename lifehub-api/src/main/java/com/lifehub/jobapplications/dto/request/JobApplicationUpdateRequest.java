package com.lifehub.jobapplications.dto.request;

import com.lifehub.jobapplications.entity.JobApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record JobApplicationUpdateRequest(
        @NotNull Long companyId,
        @NotBlank @Size(max = 200) String positionTitle,
        @Size(max = 500) String applyUrl,
        @NotNull JobApplicationStatus status,
        LocalDate appliedAt,
        @Size(max = 2000) String notes
) {
}

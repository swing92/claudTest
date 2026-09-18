package com.lifehub.jobapplications.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 100) String industry,
        @Size(max = 500) String url,
        @Size(max = 2000) String notes
) {
}

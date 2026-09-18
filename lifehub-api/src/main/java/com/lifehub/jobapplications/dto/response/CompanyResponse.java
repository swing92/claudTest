package com.lifehub.jobapplications.dto.response;

import com.lifehub.jobapplications.entity.Company;
import java.time.LocalDateTime;

public record CompanyResponse(
        Long id,
        String name,
        String industry,
        String url,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getIndustry(),
                company.getUrl(),
                company.getNotes(),
                company.getCreatedAt(),
                company.getUpdatedAt());
    }
}
